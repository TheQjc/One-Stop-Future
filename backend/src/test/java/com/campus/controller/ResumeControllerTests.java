package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.preview.DocxPreviewGenerator;
import com.campus.preview.ResumePreviewService;

@SpringBootTest(properties = {
        "spring.servlet.multipart.max-file-size=5KB",
        "spring.servlet.multipart.max-request-size=5KB"
})
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {
        "DROP TABLE IF EXISTS t_resume",
        "CREATE TABLE t_resume ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT, "
                + "user_id BIGINT NOT NULL, "
                + "title VARCHAR(120) NOT NULL, "
                + "file_name VARCHAR(255) NOT NULL, "
                + "file_ext VARCHAR(20) NOT NULL, "
                + "content_type VARCHAR(120) NOT NULL, "
                + "file_size BIGINT NOT NULL, "
                + "storage_key VARCHAR(500) NOT NULL, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ResumeControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");
    private static final Path PREVIEW_ROOT = Path.of(".local-storage", "previews");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResumePreviewService resumePreviewService;

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
    void authenticatedUserCanUploadListDownloadAndDeleteOwnResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "campus-resume.pdf",
                "application/pdf",
                "resume".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .param("title", "Intern Resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Intern Resume"))
                .andExpect(jsonPath("$.data.fileName").value("campus-resume.pdf"));

        Long resumeId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_resume WHERE user_id = 2 AND title = 'Intern Resume'",
                Long.class);
        String storageKey = jdbcTemplate.queryForObject(
                "SELECT storage_key FROM t_resume WHERE id = ?",
                String.class,
                resumeId);

        assertThat(Files.exists(STORAGE_ROOT.resolve(storageKey))).isTrue();

        mockMvc.perform(get("/api/resumes/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.resumes[0].title").value("Intern Resume"))
                .andExpect(jsonPath("$.data.resumes[0].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resumes[0].previewKind").value("FILE"));

        mockMvc.perform(get("/api/resumes/{id}/download", resumeId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("campus-resume.pdf")));

        mockMvc.perform(delete("/api/resumes/{id}", resumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_resume WHERE id = ?", Integer.class, resumeId);
        assertThat(count).isEqualTo(0);
        assertThat(Files.exists(STORAGE_ROOT.resolve(storageKey))).isFalse();
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanPreviewOwnPdfResumeInline() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                9001L, 2L, "Normal Resume", "resume.pdf", "pdf", "application/pdf", 100L, "seed/resume.pdf");
        Files.createDirectories(STORAGE_ROOT.resolve("seed"));
        Files.writeString(STORAGE_ROOT.resolve("seed/resume.pdf"), "%PDF-resume");

        mockMvc.perform(get("/api/resumes/9001/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanPreviewOwnDocxResumeAsPdf() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                9002L, 2L, "Docx Resume", "resume.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 100L, "seed/resume.docx");
        Files.createDirectories(STORAGE_ROOT.resolve("seed"));
        Files.write(STORAGE_ROOT.resolve("seed/resume.docx"), "docx".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/resumes/9002/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void docResumePreviewIsRejected() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                9003L, 2L, "Doc Resume", "resume.doc", "doc", "application/msword", 100L, "seed/resume.doc");

        mockMvc.perform(get("/api/resumes/9003/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("resume preview only supports pdf or docx"));
    }

    @Test
    @WithMockUser(username = "3", roles = "USER")
    void userCannotPreviewAnotherUsersResume() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                9004L, 2L, "Normal Resume", "resume.pdf", "pdf", "application/pdf", 100L, "seed/resume.pdf");

        mockMvc.perform(get("/api/resumes/9004/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resume not found"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void deletingDocxResumeDeletesGeneratedPreviewArtifact() throws Exception {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 22, 11, 0);
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                9005L, 2L, "Docx Resume", "resume.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 100L,
                "seed/resume.docx", updatedAt, updatedAt);
        Path artifactPath = PREVIEW_ROOT.resolve(resumePreviewService.docxArtifactKeyOf(
                resume(9005L, "resume.docx", "docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "seed/resume.docx", 100L, updatedAt)));
        Files.createDirectories(artifactPath.getParent());
        Files.writeString(artifactPath, "%PDF-preview");

        mockMvc.perform(delete("/api/resumes/9005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(Files.exists(artifactPath)).isFalse();
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void resumeUploadRejectsUnsupportedFileTypes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portfolio.zip",
                "application/zip",
                "zip".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .param("title", "Zip Resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("unsupported resume file type"));
    }

    @Test
    @WithMockUser(username = "3", roles = "USER")
    void userCannotDeleteAnotherUsersResume() throws Exception {
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                9001L, 2L, "Normal Resume", "resume.pdf", "pdf", "application/pdf", 100L, "seed/resume.pdf");

        mockMvc.perform(delete("/api/resumes/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resume not found"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void resumeUploadRejectsFilesAboveConfiguredMultipartLimit() throws Exception {
        byte[] payload = new byte[6 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversized-resume.pdf",
                "application/pdf",
                payload);

        mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .param("title", "Too Large Resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("resume file is too large"));
    }

    private byte[] samplePdfBytes() {
        return "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
    }

    private com.campus.entity.Resume resume(Long id, String fileName, String fileExt, String contentType, String storageKey,
            Long fileSize, LocalDateTime updatedAt) {
        com.campus.entity.Resume resume = new com.campus.entity.Resume();
        resume.setId(id);
        resume.setFileName(fileName);
        resume.setFileExt(fileExt);
        resume.setContentType(contentType);
        resume.setStorageKey(storageKey);
        resume.setFileSize(fileSize);
        resume.setUpdatedAt(updatedAt);
        return resume;
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
}
