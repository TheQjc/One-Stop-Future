package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
class VerificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applyRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName":"Normal User","studentId":"20260009"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void applyCreatesPendingApplicationAndUpdatesProfileStatus() throws Exception {
        mockMvc.perform(post("/api/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName":"Normal User","studentId":"20260009"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.realName").value("Normal User"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));

        Integer applicationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_verification_application WHERE user_id = 2 AND status = 'PENDING'",
                Integer.class);
        String verificationStatus = jdbcTemplate.queryForObject(
                "SELECT verification_status FROM t_user WHERE id = 2", String.class);
        assertThat(applicationCount).isEqualTo(1);
        assertThat(verificationStatus).isEqualTo("PENDING");
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void applyRejectsDuplicatePendingApplication() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO t_verification_application (id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, ?, ?)",
                201L, 2L, "Normal User", "20260009", "PENDING", LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName":"Normal User","studentId":"20260009"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("pending verification application already exists"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCannotApplyForVerification() throws Exception {
        mockMvc.perform(post("/api/verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"realName":"Admin User","studentId":"20260001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("only normal users can apply for verification"));
    }
}
