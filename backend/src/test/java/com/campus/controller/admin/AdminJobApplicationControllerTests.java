package com.campus.controller.admin;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.campus.preview.DocxPreviewGenerator;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminJobApplicationControllerTests {

    private static final Path STORAGE_ROOT = Path.of(".local-storage", "resources");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private DocxPreviewGenerator docxPreviewGenerator;

    @BeforeEach
    void stubDocxPreviewGenerator() throws IOException {
        when(docxPreviewGenerator.generate(any())).thenReturn(samplePdfBytes());
    }

    @AfterEach
    void cleanLocalStorage() throws IOException {
        deleteTreeIfExists(STORAGE_ROOT);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotReadAdminApplicationsWorkbench() throws Exception {
        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotDownloadApplicationResumeSnapshot() throws Exception {
        mockMvc.perform(get("/api/admin/applications/{id}/resume/download", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotPreviewApplicationResumeSnapshot() throws Exception {
        mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanReadApplicationsWorkbenchAndDownloadSnapshot() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/resumes/intern-resume.pdf");
        long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.pdf",
                "application/pdf", "seed/applications/intern-resume-snapshot.pdf");
        writeStoredFile("seed/applications/intern-resume-snapshot.pdf", "snapshot");

        mockMvc.perform(get("/api/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.submittedToday").value(1))
                .andExpect(jsonPath("$.data.uniqueApplicants").value(1))
                .andExpect(jsonPath("$.data.uniqueJobs").value(1))
                .andExpect(jsonPath("$.data.applications[0].id").value(applicationId))
                .andExpect(jsonPath("$.data.applications[0].jobTitle").value("Java Backend Intern"))
                .andExpect(jsonPath("$.data.applications[0].applicantNickname").value("NormalUser"))
                .andExpect(jsonPath("$.data.applications[0].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.applications[0].previewKind").value("FILE"));

        mockMvc.perform(get("/api/admin/applications/{id}/resume/download", applicationId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("intern-resume.pdf")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanPreviewPdfSnapshotInline() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/resumes/intern-resume.pdf");
        long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.pdf",
                "application/pdf", "seed/applications/intern-resume.pdf");
        writeStoredFile("seed/applications/intern-resume.pdf", "%PDF-snapshot");

        mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", applicationId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanPreviewDocxSnapshotAsPdf() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "seed/resumes/intern-resume.docx");
        long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "seed/applications/intern-resume.docx");
        writeStoredFile("seed/applications/intern-resume.docx", "docx");

        mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", applicationId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminDocSnapshotPreviewIsRejected() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.doc", "application/msword",
                "seed/resumes/intern-resume.doc");
        long applicationId = insertApplication(1L, 2L, resumeId, "Intern Resume", "intern-resume.doc",
                "application/msword", "seed/applications/intern-resume.doc");

        mockMvc.perform(get("/api/admin/applications/{id}/resume/preview", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("application resume preview only supports pdf or docx"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void snapshotDownloadStillWorksAfterOriginalResumeDeletion() throws Exception {
        long resumeId = insertResume(2L, "Intern Resume", "intern-resume.pdf", "application/pdf",
                "seed/resumes/intern-resume.pdf");
        writeStoredFile("seed/resumes/intern-resume.pdf", "original");

        mockMvc.perform(post("/api/jobs/{id}/apply", 1L)
                        .with(user("2").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId": %d}
                                """.formatted(resumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/resumes/{id}", resumeId)
                        .with(user("2").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Long applicationId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_job_application WHERE job_id = 1 AND applicant_user_id = 2",
                Long.class);

        mockMvc.perform(get("/api/admin/applications/{id}/resume/download", applicationId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("intern-resume.pdf")));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminApplicationsWorkbenchExposesNoMutationRoute() throws Exception {
        mockMvc.perform(post("/api/admin/applications/1/publish"))
                .andExpect(status().isNotFound());
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

    private byte[] samplePdfBytes() {
        return "%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII);
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
