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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import com.campus.config.ResourcePreviewProperties;
import com.campus.dto.AdminResourcePreviewMigrationRequest;
import com.campus.dto.AdminResourcePreviewMigrationResponse;
import com.campus.entity.ResourceItem;
import com.campus.entity.User;
import com.campus.mapper.ResourceItemMapper;
import com.campus.preview.DocxPreviewGenerator;
import com.campus.preview.PptxPreviewGenerator;
import com.campus.preview.ResourcePreviewArtifactStorage;
import com.campus.preview.ResourcePreviewService;
import com.campus.preview.ZipPreviewGenerator;
import com.campus.storage.MinioObjectOperations;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminResourcePreviewMigrationServiceTests {

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
    void dryRunMarksPreviewArtifactReadyWhenLocalArtifactExistsAndObjectIsMissing() throws Exception {
        ResourceItem resource = resource(7L, "Campus Deck", "PUBLISHED", "deck.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists(eq("campus-platform"), anyString())).thenReturn(false);

        String artifactKey = previewService().pptxArtifactKeyOf(resource);
        writeLocalArtifact(artifactKey, "preview");

        AdminResourcePreviewMigrationResponse response = service(true, Optional.of(minioObjectOperations))
                .migratePreviewArtifacts("1",
                        new AdminResourcePreviewMigrationRequest(true, List.of("published", "published"),
                                List.of(7L, 7L), " deck ", true, 100));

        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).previewType()).isEqualTo("PPTX");
        assertThat(response.items().get(0).artifactKey()).isEqualTo(artifactKey);
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
    void unsupportedPreviewTypeIsSkipped() throws Exception {
        ResourceItem resource = resource(8L, "Campus Guide", "PUBLISHED", "guide.pdf", "pdf", "application/pdf");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);

        AdminResourcePreviewMigrationResponse response = service(true, Optional.of(minioObjectOperations))
                .migratePreviewArtifacts("1",
                        new AdminResourcePreviewMigrationRequest(true, null, null, null, true, 100));

        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.previewType()).isEqualTo("NONE");
            assertThat(item.artifactKey()).isNull();
            assertThat(item.outcome()).isEqualTo("SKIPPED");
            assertThat(item.message()).isEqualTo("preview not supported");
        });
    }

    @Test
    void missingLocalPreviewArtifactIsSkipped() throws Exception {
        ResourceItem resource = resource(9L, "Workbook", "PUBLISHED", "workbook.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);

        AdminResourcePreviewMigrationResponse response = service(true, Optional.of(minioObjectOperations))
                .migratePreviewArtifacts("1",
                        new AdminResourcePreviewMigrationRequest(true, null, null, null, true, 100));

        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.previewType()).isEqualTo("DOCX");
            assertThat(item.outcome()).isEqualTo("SKIPPED");
            assertThat(item.message()).isEqualTo("local preview artifact not found");
        });
    }

    @Test
    void existingMinioPreviewArtifactIsSkippedWhenOnlyMissingInMinioIsTrue() throws Exception {
        ResourceItem resource = resource(10L, "Archive", "PUBLISHED", "archive.zip", "zip", "application/zip");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(resource));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);

        String artifactKey = previewService().zipArtifactKeyOf(resource);
        writeLocalArtifact(artifactKey, "{\"entries\":[]}");
        when(minioObjectOperations.objectExists("campus-platform", "preview-artifacts/" + artifactKey)).thenReturn(true);

        AdminResourcePreviewMigrationResponse response = service(true, Optional.of(minioObjectOperations))
                .migratePreviewArtifacts("1",
                        new AdminResourcePreviewMigrationRequest(true, null, null, null, true, 100));

        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.previewType()).isEqualTo("ZIP");
            assertThat(item.artifactKey()).isEqualTo(artifactKey);
            assertThat(item.outcome()).isEqualTo("SKIPPED");
            assertThat(item.message()).isEqualTo("preview artifact already exists in minio");
        });
    }

    @Test
    void executeContinuesAfterOnePreviewArtifactFailsToUpload() throws Exception {
        ResourceItem first = resource(11L, "First Deck", "PUBLISHED", "first.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        ResourceItem second = resource(12L, "Second Deck", "PUBLISHED", "second.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(first, second));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists(anyString(), anyString())).thenReturn(false);

        String firstArtifactKey = previewService().pptxArtifactKeyOf(first);
        String secondArtifactKey = previewService().pptxArtifactKeyOf(second);
        writeLocalArtifact(firstArtifactKey, "first");
        writeLocalArtifact(secondArtifactKey, "second");

        org.mockito.Mockito.doThrow(new IOException("boom")).when(minioObjectOperations)
                .putObject(eq("campus-platform"), eq("preview-artifacts/" + firstArtifactKey), any(InputStream.class));

        AdminResourcePreviewMigrationResponse response = service(true, Optional.of(minioObjectOperations))
                .migratePreviewArtifacts("1",
                        new AdminResourcePreviewMigrationRequest(false, null, null, null, true, 100));

        assertThat(response.processedCount()).isEqualTo(2);
        assertThat(response.failureCount()).isEqualTo(1);
        assertThat(response.successCount()).isEqualTo(1);
        assertThat(response.items()).extracting(AdminResourcePreviewMigrationResponse.Item::outcome)
                .containsExactly("FAILED", "SUCCESS");
        assertThat(response.items()).extracting(AdminResourcePreviewMigrationResponse.Item::message)
                .containsExactly("failed to upload preview artifact", "uploaded to minio");

        verify(minioObjectOperations).putObject(eq("campus-platform"), eq("preview-artifacts/" + secondArtifactKey),
                any(InputStream.class));
    }

    @Test
    void migrationFailsFastWhenMinioPreviewMigrationIsUnavailable() {
        when(userService.requireByIdentity("1")).thenReturn(adminUser());

        AdminResourcePreviewMigrationService service = service(false, Optional.empty());

        assertThatThrownBy(() -> service.migratePreviewArtifacts("1",
                new AdminResourcePreviewMigrationRequest(true, null, null, null, true, 100)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(500);
                    assertThat(exception).hasMessage("minio preview migration unavailable");
                });

        verifyNoInteractions(resourceItemMapper);
    }

    @Test
    void requestWithoutKeywordStillUsesDeterministicBoundedBatch() throws Exception {
        ResourceItem first = resource(13L, "First Workbook", "PUBLISHED", "first.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        ResourceItem second = resource(14L, "Second Workbook", "PUBLISHED", "second.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(resourceItemMapper.selectList(any())).thenReturn(List.of(first, second));
        when(userService.requireByIdentity("1")).thenReturn(adminUser());
        when(minioObjectOperations.bucketExists("campus-platform")).thenReturn(true);
        when(minioObjectOperations.objectExists(anyString(), anyString())).thenReturn(false);

        writeLocalArtifact(previewService().docxArtifactKeyOf(first), "first");
        writeLocalArtifact(previewService().docxArtifactKeyOf(second), "second");

        AdminResourcePreviewMigrationResponse response = service(true, Optional.of(minioObjectOperations))
                .migratePreviewArtifacts("1",
                        new AdminResourcePreviewMigrationRequest(null, null, null, null, null, 2));

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

    private AdminResourcePreviewMigrationService service(boolean minioEnabled,
            Optional<MinioObjectOperations> operations) {
        ResourcePreviewProperties resourcePreviewProperties = new ResourcePreviewProperties();
        resourcePreviewProperties.setLocalRoot(tempDir.toString());
        resourcePreviewProperties.setMinioPrefix("preview-artifacts");

        MinioIntegrationProperties minioIntegrationProperties = new MinioIntegrationProperties();
        minioIntegrationProperties.setEnabled(minioEnabled);
        minioIntegrationProperties.setBucket("campus-platform");

        return new AdminResourcePreviewMigrationService(resourceItemMapper, userService, previewService(),
                resourcePreviewProperties, minioIntegrationProperties, operations);
    }

    private ResourcePreviewService previewService() {
        ResourcePreviewArtifactStorage unusedStorage = new ResourcePreviewArtifactStorage() {
            @Override
            public boolean exists(String artifactKey) {
                return false;
            }

            @Override
            public InputStream open(String artifactKey) throws IOException {
                throw new IOException("unused");
            }

            @Override
            public void write(String artifactKey, InputStream inputStream) {
            }

            @Override
            public void delete(String artifactKey) {
            }
        };
        PptxPreviewGenerator pptxGenerator = inputStream -> new byte[0];
        DocxPreviewGenerator docxGenerator = inputStream -> new byte[0];
        ZipPreviewGenerator zipGenerator = (resourceId, fileName, inputStream) -> null;
        return new ResourcePreviewService(unusedStorage, new ObjectMapper(), pptxGenerator, docxGenerator, zipGenerator);
    }

    private void writeLocalArtifact(String artifactKey, String body) throws IOException {
        Path storedFile = tempDir.resolve(artifactKey);
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, body);
    }

    private ResourceItem resource(Long id, String title, String status, String storageKey, String fileExt,
            String contentType) {
        ResourceItem resource = new ResourceItem();
        resource.setId(id);
        resource.setTitle(title);
        resource.setStatus(status);
        resource.setStorageKey(storageKey);
        resource.setFileName(title + "." + fileExt);
        resource.setFileExt(fileExt);
        resource.setContentType(contentType);
        resource.setFileSize(1024L);
        resource.setSummary(title + " summary");
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
