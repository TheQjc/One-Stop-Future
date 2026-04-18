package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
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
                .andExpect(jsonPath("$.data.resumes[0].title").value("Intern Resume"));

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
