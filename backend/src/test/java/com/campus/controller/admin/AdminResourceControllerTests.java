package com.campus.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.dto.AdminResourceMigrationResponse;
import com.campus.preview.DocxPreviewGenerator;
import com.campus.service.AdminResourceMigrationService;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminResourceControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");
    private static final Path PREVIEW_ROOT = Path.of(".local-storage", "previews");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AdminResourceMigrationService adminResourceMigrationService;

    @MockBean
    private DocxPreviewGenerator docxPreviewGenerator;

    @BeforeEach
    void stubDocxPreviewGenerator() throws IOException {
        when(docxPreviewGenerator.generate(any())).thenReturn(samplePdfBytes());
    }

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
        deleteTreeIfExists(PREVIEW_ROOT);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotOpenAdminResourcesList() throws Exception {
        mockMvc.perform(get("/api/admin/resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminListReturnsAllResources() throws Exception {
        mockMvc.perform(get("/api/admin/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.resources[0].status").isNotEmpty())
                .andExpect(jsonPath("$.data.resources[0].previewKind").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotTriggerHistoricalMinioMigration() throws Exception {
        mockMvc.perform(post("/api/admin/resources/migrate-to-minio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanDryRunHistoricalMinioMigration() throws Exception {
        when(adminResourceMigrationService.migrateResources(eq("1"), any()))
                .thenReturn(new AdminResourceMigrationResponse(
                        true,
                        100,
                        1,
                        1,
                        1,
                        0,
                        0,
                        List.of(new AdminResourceMigrationResponse.Item(
                                7L,
                                "Campus Resume",
                                "PUBLISHED",
                                "2026/04/17/resume.pdf",
                                "SUCCESS",
                                "ready to migrate"))));

        mockMvc.perform(post("/api/admin/resources/migrate-to-minio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dryRun":true,"statuses":["PUBLISHED"],"keyword":"resume","limit":100}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.requestedLimit").value(100))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.items[0].resourceId").value(7))
                .andExpect(jsonPath("$.data.items[0].message").value("ready to migrate"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void invalidMigrationLimitReturnsBodyCode400() throws Exception {
        mockMvc.perform(post("/api/admin/resources/migrate-to-minio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"limit":201}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminResourcesExposePreviewAvailability() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resource_item (
                          id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                          file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                          published_at, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                4L,
                "Admin Preview Resource",
                "RESUME_TEMPLATE",
                "Admin preview summary",
                "Admin preview description",
                "REJECTED",
                2L,
                1L,
                "Needs copy revision",
                "admin-preview.pdf",
                "pdf",
                "application/pdf",
                2048L,
                "seed/2026/04/admin-preview.pdf",
                0,
                0,
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusSeconds(4),
                LocalDateTime.now().plusSeconds(5));

        mockMvc.perform(get("/api/admin/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.resources[0].id").value(4))
                .andExpect(jsonPath("$.data.resources[0].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resources[0].previewKind").value("FILE"))
                .andExpect(jsonPath("$.data.resources[1].id").value(3))
                .andExpect(jsonPath("$.data.resources[1].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resources[1].previewKind").value("FILE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanPublishRejectAndOfflineResources() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resource_item (
                          id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                          file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                          published_at, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?)
                        """,
                4L, "Mock Exam Paper", "EXAM_PAPER", "Pending exam paper", "Pending exam paper details",
                "PENDING", 2L, "mock-exam-paper.pdf", "pdf", "application/pdf", 2048L,
                "seed/2026/04/mock-exam-paper.pdf", 0, 0,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(2));

        mockMvc.perform(post("/api/admin/resources/4/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        mockMvc.perform(post("/api/admin/resources/3/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Need clearer naming"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

        mockMvc.perform(post("/api/admin/resources/1/offline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));

        mockMvc.perform(get("/api/resources/1").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM t_resource_item WHERE id = 3", String.class);
        assertThat(statusValue).isEqualTo("REJECTED");
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanPreviewPendingPptxAsInlinePdf() throws Exception {
        insertResource(4L, 2L, "PENDING", "admin-preview.pptx", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "seed/2026/04/admin-preview.pptx");
        writeStoredBinaryFile("seed/2026/04/admin-preview.pptx", simplePptxBytes("Admin PPTX"));

        mockMvc.perform(get("/api/resources/4/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanPreviewPendingDocxAsInlinePdf() throws Exception {
        writeStoredBinaryFile("seed/2026/04/ielts-writing-drill.docx", "docx".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/resources/3/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    private void writeStoredBinaryFile(String storageKey, byte[] content) throws IOException {
        Path filePath = STORAGE_ROOT.resolve(storageKey);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);
    }

    private byte[] simplePptxBytes(String title) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(48, 48, 600, 100));
            XSLFTextParagraph paragraph = textBox.addNewTextParagraph();
            XSLFTextRun textRun = paragraph.addNewTextRun();
            textRun.setText(title);
            slideShow.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] samplePdfBytes() {
        return """
                %PDF-1.4
                1 0 obj
                <<>>
                endobj
                trailer
                <<>>
                %%EOF
                """.getBytes(StandardCharsets.UTF_8);
    }

    private void deleteTreeIfExists(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }

    private void insertResource(Long id, Long uploaderId, String status, String fileName, String fileExt,
            String contentType, String storageKey) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO t_resource_item (
                          id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                          file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                          published_at, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                "Inserted Resource " + id,
                "RESUME_TEMPLATE",
                "Inserted resource summary",
                "Inserted resource description",
                status,
                uploaderId,
                null,
                null,
                fileName,
                fileExt,
                contentType,
                2048L,
                storageKey,
                0,
                0,
                "PUBLISHED".equals(status) ? now.minusHours(2) : null,
                "PUBLISHED".equals(status) || "REJECTED".equals(status) || "OFFLINE".equals(status) ? now.minusHours(2)
                        : null,
                now.plusSeconds(id),
                now.plusSeconds(id + 1));
    }
}
