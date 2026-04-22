package com.campus.controller.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AdminDashboardControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotAccessDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminDashboardSummaryReturnsCountsAndRecentRows() throws Exception {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        clearDashboardTables();

        insertPendingApplication(301L, 2L, "Normal User", "20260009", startOfToday.plusHours(1));
        insertPendingApplication(302L, 3L, "Verified User", "20260010", startOfToday.plusHours(1));
        insertReviewedApplication(303L, 2L, "Normal User", "20260011", startOfToday.minusDays(1).plusHours(20),
                startOfToday.plusHours(2));
        insertReviewedApplication(304L, 3L, "Verified User", "20260012", startOfToday.minusDays(1).plusHours(18),
                startOfToday.minusSeconds(1));

        insertCommunityPost(401L, 2L, "PUBLISHED", startOfToday.plusHours(3));
        insertCommunityPost(402L, 2L, "HIDDEN", startOfToday.plusHours(4));
        insertCommunityPost(403L, 3L, "DELETED", startOfToday.plusHours(4));

        insertJob(501L, "DRAFT", startOfToday.plusHours(5), startOfToday.plusHours(7));
        insertJob(502L, "OFFLINE", startOfToday.plusHours(6), startOfToday.plusHours(7));
        insertJob(503L, "PUBLISHED", startOfToday.plusHours(2), startOfToday.plusHours(8));
        insertJob(504L, "DELETED", startOfToday.plusHours(1), startOfToday.plusHours(9));

        insertResource(601L, 2L, "PENDING", "pending-notes.docx", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", startOfToday.plusHours(8));
        insertResource(602L, 3L, "PENDING", "pending-preview.pdf", "pdf", "application/pdf",
                startOfToday.plusHours(8));
        insertResource(603L, 2L, "PUBLISHED", "published.pdf", "pdf", "application/pdf", startOfToday.plusHours(2));
        insertResource(604L, 2L, "REJECTED", "rejected.zip", "zip", "application/zip", startOfToday.plusHours(9));
        insertResource(605L, 3L, "OFFLINE", "offline.pdf", "pdf", "application/pdf", startOfToday.plusHours(10));

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.verification.pendingCount").value(2))
                .andExpect(jsonPath("$.data.verification.reviewedToday").value(1))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications.length()").value(2))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications[0].id").value(302))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications[1].id").value(301))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications[0].applicantNickname")
                        .value("VerifiedUser"))
                .andExpect(jsonPath("$.data.community.totalCount").value(3))
                .andExpect(jsonPath("$.data.community.publishedCount").value(1))
                .andExpect(jsonPath("$.data.community.hiddenCount").value(1))
                .andExpect(jsonPath("$.data.community.deletedCount").value(1))
                .andExpect(jsonPath("$.data.community.latestPosts.length()").value(3))
                .andExpect(jsonPath("$.data.community.latestPosts[0].id").value(403))
                .andExpect(jsonPath("$.data.community.latestPosts[1].id").value(402))
                .andExpect(jsonPath("$.data.community.latestPosts[2].id").value(401))
                .andExpect(jsonPath("$.data.community.latestPosts[0].status").value("DELETED"))
                .andExpect(jsonPath("$.data.jobs.totalCount").value(3))
                .andExpect(jsonPath("$.data.jobs.draftCount").value(1))
                .andExpect(jsonPath("$.data.jobs.publishedCount").value(1))
                .andExpect(jsonPath("$.data.jobs.offlineCount").value(1))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs.length()").value(2))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[0].id").value(502))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[1].id").value(501))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[0].status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.resources.totalCount").value(5))
                .andExpect(jsonPath("$.data.resources.pendingCount").value(2))
                .andExpect(jsonPath("$.data.resources.publishedCount").value(1))
                .andExpect(jsonPath("$.data.resources.closedCount").value(2))
                .andExpect(jsonPath("$.data.resources.latestPendingResources.length()").value(2))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[0].id").value(602))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[1].id").value(601))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[0].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[0].previewKind").value("FILE"))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[1].previewAvailable").value(true))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[1].previewKind").value("FILE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminDashboardSummaryKeepsEmptyRecentLists() throws Exception {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        clearDashboardTables();

        insertReviewedApplication(301L, 2L, "Normal User", "20260009", startOfToday.minusHours(2),
                startOfToday.plusHours(1));
        insertCommunityPost(401L, 2L, "PUBLISHED", startOfToday.plusHours(2));
        insertJob(501L, "PUBLISHED", startOfToday.plusHours(3), startOfToday.plusHours(4));
        insertResource(601L, 2L, "PUBLISHED", "published.pdf", "pdf", "application/pdf", startOfToday.plusHours(5));

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.verification.pendingCount").value(0))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications.length()").value(0))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs.length()").value(0))
                .andExpect(jsonPath("$.data.resources.latestPendingResources.length()").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminDashboardSummaryCapsEachRecentListAtFiveItems() throws Exception {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        clearDashboardTables();

        for (long index = 0; index < 6; index++) {
            LocalDateTime timestamp = startOfToday.plusMinutes(index);
            insertPendingApplication(500L + index, 2L, "Normal User", "20261" + index, timestamp);
            insertCommunityPost(600L + index, 2L, "PUBLISHED", timestamp);
            insertJob(700L + index, index % 2 == 0 ? "DRAFT" : "OFFLINE", timestamp, timestamp);
            insertResource(800L + index, 2L, "PENDING", "pending-" + index + ".pdf", "pdf", "application/pdf",
                    timestamp);
        }

        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verification.latestPendingApplications.length()").value(5))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications[0].id").value(505))
                .andExpect(jsonPath("$.data.verification.latestPendingApplications[4].id").value(501))
                .andExpect(jsonPath("$.data.community.latestPosts.length()").value(5))
                .andExpect(jsonPath("$.data.community.latestPosts[0].id").value(605))
                .andExpect(jsonPath("$.data.community.latestPosts[4].id").value(601))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs.length()").value(5))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[0].id").value(705))
                .andExpect(jsonPath("$.data.jobs.latestActionableJobs[4].id").value(701))
                .andExpect(jsonPath("$.data.resources.latestPendingResources.length()").value(5))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[0].id").value(805))
                .andExpect(jsonPath("$.data.resources.latestPendingResources[4].id").value(801));
    }

    private void clearDashboardTables() {
        jdbcTemplate.update("DELETE FROM t_verification_application");
        jdbcTemplate.update("DELETE FROM t_community_post");
        jdbcTemplate.update("DELETE FROM t_job_posting");
        jdbcTemplate.update("DELETE FROM t_resource_item");
    }

    private void insertPendingApplication(Long id, Long userId, String realName, String studentId, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE t_user SET verification_status = 'PENDING' WHERE id = ?", userId);
        jdbcTemplate.update(
                """
                        INSERT INTO t_verification_application (
                          id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, ?, ?)
                        """,
                id, userId, realName, studentId, "PENDING", createdAt, createdAt.plusMinutes(1));
    }

    private void insertReviewedApplication(Long id, Long userId, String realName, String studentId, LocalDateTime createdAt,
            LocalDateTime reviewedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_verification_application (
                          id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?)
                        """,
                id, userId, realName, studentId, "APPROVED", 1L, reviewedAt, createdAt, reviewedAt);
    }

    private void insertCommunityPost(Long id, Long authorId, String status, LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_community_post (
                          id, author_id, tag, title, content, status, like_count, comment_count, favorite_count, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, authorId, "CAREER", "Post " + id, "Content for post " + id, status, 10, 3, 2, createdAt,
                createdAt.plusMinutes(1));
    }

    private void insertJob(Long id, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_job_posting (
                          id, title, company_name, city, job_type, education_requirement, source_platform, source_url,
                          summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                "Job " + id,
                "Company " + id,
                "Shanghai",
                "INTERNSHIP",
                "BACHELOR",
                "Official Site",
                "https://jobs.example.com/" + id,
                "Summary for job " + id,
                "Content for job " + id,
                createdAt.plusDays(30),
                "PUBLISHED".equals(status) || "OFFLINE".equals(status) ? createdAt.plusMinutes(10) : null,
                status,
                1L,
                1L,
                createdAt,
                updatedAt);
    }

    private void insertResource(Long id, Long uploaderId, String status, String fileName, String fileExt, String contentType,
            LocalDateTime createdAt) {
        LocalDateTime reviewedAt = "REJECTED".equals(status) || "OFFLINE".equals(status) ? createdAt.plusMinutes(2) : null;
        LocalDateTime publishedAt = "PUBLISHED".equals(status) || "OFFLINE".equals(status) ? createdAt.plusMinutes(1) : null;
        String rejectReason = "REJECTED".equals(status) ? "Needs changes" : null;

        jdbcTemplate.update(
                """
                        INSERT INTO t_resource_item (
                          id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                          file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                          published_at, reviewed_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                "Resource " + id,
                "RESUME_TEMPLATE",
                "Summary for resource " + id,
                "Description for resource " + id,
                status,
                uploaderId,
                reviewedAt == null ? null : 1L,
                rejectReason,
                fileName,
                fileExt,
                contentType,
                2048L,
                "seed/2026/04/" + fileName,
                7,
                0,
                publishedAt,
                reviewedAt,
                createdAt,
                createdAt.plusMinutes(3));
    }
}
