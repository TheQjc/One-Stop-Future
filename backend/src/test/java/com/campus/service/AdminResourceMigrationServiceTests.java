package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.common.BusinessException;
import com.campus.config.MinioIntegrationProperties;
import com.campus.config.ResourceStorageProperties;
import com.campus.dto.AdminResourceMigrationRequest;
import com.campus.dto.AdminResourceMigrationResponse;
import com.campus.entity.ResourceItem;
import com.campus.entity.User;
import com.campus.mapper.ResourceItemMapper;
import com.campus.storage.MinioObjectOperations;

@ExtendWith(MockitoExtension.class)
class AdminResourceMigrationServiceTests {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.remove(ResourceItem.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ResourceItem.class);
    }

    @Mock
    private ResourceItemMapper resourceItemMapper;

    @Mock
    private UserService userService;

    @Mock
    private MinioObjectOperations minioObjectOperations;

    @TempDir
    Path tempDir;

    @Test
    void dryRunMarksResourceReadyWhenLocalFileExistsAndObjectIsMissing() throws Exception {
        ResourceItem resource = resource(7L, "Campus Resume", "PUBLISHED", "2026/04/17/resume.pdf");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists("campus-platform", "2026/04/17/resume.pdf")).thenReturn(false);

        writeLegacyFile("2026/04/17/resume.pdf", "legacy");

        AdminResourceMigrationService service = service(tempDir, true, Optional.of(minioObjectOperations));

        AdminResourceMigrationResponse response = service.migrateResources("1",
                new AdminResourceMigrationRequest(true, List.of("published"), List.of(7L), " resume ", true, 100));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).outcome()).isEqualTo("SUCCESS");
        assertThat(response.items().get(0).message()).isEqualTo("ready to migrate");

        ArgumentCaptor<LambdaQueryWrapper<ResourceItem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(resourceItemMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment().toLowerCase(Locale.ROOT);
        assertThat(sqlSegment).contains("status in")
                .contains("id in")
                .contains("lower(title) like")
                .contains("lower(summary) like")
                .contains("lower(file_name) like")
                .contains("order by id asc")
                .contains("limit 100");
    }

    @Test
    void executeContinuesAfterOneResourceFailsToUpload() throws Exception {
        ResourceItem first = resource(7L, "First Resume", "PUBLISHED", "2026/04/17/first.pdf");
        ResourceItem second = resource(8L, "Second Resume", "PUBLISHED", "2026/04/17/second.pdf");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(first, second));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists(anyString(), anyString())).thenReturn(false);
        org.mockito.Mockito.doThrow(new IOException("boom")).when(minioObjectOperations)
                .putObject(eq("campus-platform"), eq("2026/04/17/first.pdf"), any(InputStream.class));

        writeLegacyFile("2026/04/17/first.pdf", "first");
        writeLegacyFile("2026/04/17/second.pdf", "second");

        AdminResourceMigrationResponse response = service(tempDir, true, Optional.of(minioObjectOperations))
                .migrateResources("1", new AdminResourceMigrationRequest(false, null, null, null, true, 100));

        assertThat(response.processedCount()).isEqualTo(2);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.items()).extracting(AdminResourceMigrationResponse.Item::outcome)
                .containsExactly("FAILED", "SUCCESS");
        assertThat(response.items()).extracting(AdminResourceMigrationResponse.Item::message)
                .containsExactly("failed to upload to minio", "uploaded to minio");

        verify(minioObjectOperations).putObject(eq("campus-platform"), eq("2026/04/17/second.pdf"),
                any(InputStream.class));
    }

    @Test
    void migrationFailsWhenMinioIntegrationIsDisabled() {
        when(userService.requireByIdentity("1")).thenReturn(adminUser());

        AdminResourceMigrationService service = service(tempDir, false, Optional.empty());

        assertThatThrownBy(() -> service.migrateResources("1",
                new AdminResourceMigrationRequest(true, null, null, null, true, 100)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("minio migration unavailable");
                });

        verifyNoInteractions(resourceItemMapper);
    }

    @Test
    void requestWithoutKeywordStillUsesDeterministicBoundedBatch() throws Exception {
        ResourceItem first = resource(7L, "First", "PUBLISHED", "2026/04/17/first.pdf");
        ResourceItem second = resource(8L, "Second", "PUBLISHED", "2026/04/17/second.pdf");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(first, second));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists(anyString(), anyString())).thenReturn(false);

        writeLegacyFile("2026/04/17/first.pdf", "first");
        writeLegacyFile("2026/04/17/second.pdf", "second");

        AdminResourceMigrationResponse response = service(tempDir, true, Optional.of(minioObjectOperations))
                .migrateResources("1", new AdminResourceMigrationRequest(null, null, null, null, null, 2));

        assertThat(response.dryRun()).isTrue();
        assertThat(response.requestedLimit()).isEqualTo(2);
        assertThat(response.matchedCount()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);

        ArgumentCaptor<LambdaQueryWrapper<ResourceItem>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(resourceItemMapper).selectList(captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment().toLowerCase(Locale.ROOT);
        assertThat(sqlSegment).doesNotContain("lower(title) like")
                .contains("order by id asc")
                .contains("limit 2");
    }

    private AdminResourceMigrationService service(Path localRoot, boolean minioEnabled,
            Optional<MinioObjectOperations> operations) {
        ResourceStorageProperties resourceStorageProperties = new ResourceStorageProperties();
        resourceStorageProperties.setLocalRoot(localRoot.toString());

        MinioIntegrationProperties minioIntegrationProperties = new MinioIntegrationProperties();
        minioIntegrationProperties.setEnabled(minioEnabled);
        minioIntegrationProperties.setBucket("campus-platform");

        return new AdminResourceMigrationService(resourceItemMapper, userService, resourceStorageProperties,
                minioIntegrationProperties, operations);
    }

    private void writeLegacyFile(String storageKey, String body) throws IOException {
        Path storedFile = tempDir.resolve(storageKey);
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, body);
    }

    private ResourceItem resource(Long id, String title, String status, String storageKey) {
        ResourceItem resource = new ResourceItem();
        resource.setId(id);
        resource.setTitle(title);
        resource.setStatus(status);
        resource.setStorageKey(storageKey);
        resource.setSummary(title + " summary");
        resource.setFileName(title + ".pdf");
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        return resource;
    }

    private User adminUser() {
        User user = new User();
        user.setId(1L);
        user.setRole("ADMIN");
        return user;
    }
}
