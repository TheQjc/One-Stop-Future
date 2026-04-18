package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class JobApplicationControllerTests {

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
    void userCanApplyOnceAndListOwnApplications() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/resumes/intern-resume.pdf");
        writeStoredFile("seed/resumes/intern-resume.pdf", "resume-pdf");

        mockMvc.perform(post("/api/jobs/{id}/apply", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.jobId").value(1))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.resumeTitleSnapshot").value("Intern Resume"));

        Integer applicationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_application WHERE job_id = 1 AND applicant_user_id = 2",
                Integer.class);
        assertThat(applicationCount).isEqualTo(1);

        String snapshotStorageKey = jdbcTemplate.queryForObject(
                "SELECT resume_storage_key_snapshot FROM t_job_application WHERE job_id = 1 AND applicant_user_id = 2",
                String.class);
        assertThat(snapshotStorageKey).isNotBlank();
        assertThat(snapshotStorageKey).isNotEqualTo("seed/resumes/intern-resume.pdf");
        assertThat(Files.readString(STORAGE_ROOT.resolve(snapshotStorageKey), StandardCharsets.UTF_8))
                .isEqualTo("resume-pdf");

        mockMvc.perform(get("/api/applications/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.applications[0].jobId").value(1))
                .andExpect(jsonPath("$.data.applications[0].jobTitle").value("Java Backend Intern"))
                .andExpect(jsonPath("$.data.applications[0].resumeTitleSnapshot").value("Intern Resume"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void duplicateApplyToSameJobIsRejected() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/resumes/intern-resume.pdf");
        writeStoredFile("seed/resumes/intern-resume.pdf", "resume-pdf");
        insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/applications/existing-snapshot.pdf");

        mockMvc.perform(post("/api/jobs/{id}/apply", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("already applied to this job"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void applyingWithAnotherUsersResumeIsRejected() throws Exception {
        long resumeId = insertResume(3L, "Foreign Resume", "foreign.pdf", "application/pdf",
                "seed/resumes/foreign.pdf");

        mockMvc.perform(post("/api/jobs/{id}/apply", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("resume not found"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void applyingToNonPublishedJobIsRejected() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/resumes/intern-resume.pdf");

        mockMvc.perform(post("/api/jobs/{id}/apply", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("job not found"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void snapshotCopyFailureDoesNotInsertApplication() throws Exception {
        long resumeId = insertResume(2L, "Missing Resume", "missing.pdf", "application/pdf",
                "seed/resumes/missing.pdf");

        mockMvc.perform(post("/api/jobs/{id}/apply", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("failed to store application resume snapshot"));

        Integer applicationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_job_application WHERE job_id = 1 AND applicant_user_id = 2",
                Integer.class);
        assertThat(applicationCount).isEqualTo(0);
    }

    private long insertResume(Long userId, String title, String fileName, String contentType, String storageKey) {
        Long id = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM t_resume", Long.class);
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO t_resume (
                          id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                userId,
                title,
                fileName,
                extensionOf(fileName),
                contentType,
                128L,
                storageKey,
                now,
                now);
        return id;
    }

    private long insertApplication(Long jobId, Long applicantUserId, Long resumeId, String resumeTitle,
            String resumeFileName, String resumeContentType, String snapshotStorageKey) {
        Long id = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM t_job_application", Long.class);
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                """
                        INSERT INTO t_job_application (
                          id, job_id, applicant_user_id, resume_id, status,
                          resume_title_snapshot, resume_file_name_snapshot, resume_file_ext_snapshot,
                          resume_content_type_snapshot, resume_file_size_snapshot, resume_storage_key_snapshot,
                          submitted_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                jobId,
                applicantUserId,
                resumeId,
                "SUBMITTED",
                resumeTitle,
                resumeFileName,
                extensionOf(resumeFileName),
                resumeContentType,
                128L,
                snapshotStorageKey,
                now,
                now,
                now);
        return id;
    }

    private void writeStoredFile(String storageKey, String content) throws IOException {
        Path filePath = STORAGE_ROOT.resolve(storageKey);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    private String extensionOf(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot < 0 ? "" : fileName.substring(lastDot + 1);
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
