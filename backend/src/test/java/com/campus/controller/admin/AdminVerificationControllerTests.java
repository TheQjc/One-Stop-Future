package com.campus.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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
class AdminVerificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void nonAdminCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/verifications/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void dashboardAndListReturnVerificationApplications() throws Exception {
        insertPendingApplication(201L, 2L, "Normal User", "20260009", LocalDateTime.now().minusHours(2));
        insertReviewedApplication(202L, 3L, "Verified User", "20260001", LocalDateTime.now().minusHours(4),
                LocalDateTime.now().minusMinutes(10));

        mockMvc.perform(get("/api/admin/verifications/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pendingCount").value(1))
                .andExpect(jsonPath("$.data.reviewedToday").value(1))
                .andExpect(jsonPath("$.data.latestPendingApplications[0].applicantNickname").value("NormalUser"));

        mockMvc.perform(get("/api/admin/verifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].status").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void approveReviewUpdatesUserStatusAndCreatesNotification() throws Exception {
        insertPendingApplication(201L, 2L, "Normal User", "20260009", LocalDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/admin/verifications/201/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"APPROVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String applicationStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM t_verification_application WHERE id = 201", String.class);
        String userVerificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM t_user WHERE id = 2", String.class);
        String studentId = jdbcTemplate.queryForObject(
                "SELECT student_id FROM t_user WHERE id = 2", String.class);
        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = 2 AND type = 'VERIFICATION_APPROVED'",
                Integer.class);

        assertThat(applicationStatus).isEqualTo("APPROVED");
        assertThat(userVerificationStatus).isEqualTo("VERIFIED");
        assertThat(studentId).isEqualTo("20260009");
        assertThat(notificationCount).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void rejectRequiresReason() throws Exception {
        insertPendingApplication(201L, 2L, "Normal User", "20260009", LocalDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/admin/verifications/201/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"REJECT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("reason is required when rejecting application"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void rejectReviewRestoresUserToUnverifiedAndCreatesNotification() throws Exception {
        insertPendingApplication(201L, 2L, "Normal User", "20260009", LocalDateTime.now().minusHours(1));
        jdbcTemplate.update("UPDATE t_user SET verification_status = 'PENDING' WHERE id = 2");

        mockMvc.perform(post("/api/admin/verifications/201/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"REJECT","reason":"student id mismatch"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String applicationStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM t_verification_application WHERE id = 201", String.class);
        String rejectReason = jdbcTemplate.queryForObject(
                "SELECT reject_reason FROM t_verification_application WHERE id = 201", String.class);
        String userVerificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM t_user WHERE id = 2", String.class);
        String studentId = jdbcTemplate.queryForObject(
                "SELECT student_id FROM t_user WHERE id = 2", String.class);
        Integer notificationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = 2 AND type = 'VERIFICATION_REJECTED'",
                Integer.class);

        assertThat(applicationStatus).isEqualTo("REJECTED");
        assertThat(rejectReason).isEqualTo("student id mismatch");
        assertThat(userVerificationStatus).isEqualTo("UNVERIFIED");
        assertThat(studentId).isNull();
        assertThat(notificationCount).isEqualTo(1);
    }

    private void insertPendingApplication(Long id, Long userId, String realName, String studentId, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE t_user SET verification_status = 'PENDING' WHERE id = ?", userId);
        jdbcTemplate.update(
                "INSERT INTO t_verification_application (id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, ?, ?)",
                id, userId, realName, studentId, "PENDING", createdAt, createdAt);
    }

    private void insertReviewedApplication(Long id, Long userId, String realName, String studentId, LocalDateTime createdAt,
            LocalDateTime reviewedAt) {
        jdbcTemplate.update(
                "INSERT INTO t_verification_application (id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NULL, ?, ?, ?, ?)",
                id, userId, realName, studentId, "APPROVED", 1L, reviewedAt, createdAt, reviewedAt);
    }
}
