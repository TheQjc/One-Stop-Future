package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class HomeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guestSummaryReturnsPublicHomeData() throws Exception {
        mockMvc.perform(get("/api/home/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.viewerType").value("GUEST"))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(0))
                .andExpect(jsonPath("$.data.discoverPreview.period").value("WEEK"))
                .andExpect(jsonPath("$.data.discoverPreview.items[0].path").value("/resources/1"))
                .andExpect(jsonPath("$.data.entries[0].code").value("community"))
                .andExpect(jsonPath("$.data.entries[1].code").value("jobs"))
                .andExpect(jsonPath("$.data.entries[1].path").value("/jobs"))
                .andExpect(jsonPath("$.data.entries[1].badge").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.entries[2].code").value("resources"))
                .andExpect(jsonPath("$.data.entries[2].path").value("/resources"))
                .andExpect(jsonPath("$.data.entries[2].badge").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.entries[3].code").value("assessment"))
                .andExpect(jsonPath("$.data.entries[3].enabled").value(false))
                .andExpect(jsonPath("$.data.entries[3].badge").value("LOGIN_REQUIRED"))
                .andExpect(jsonPath("$.data.entries[4].code").value("analytics"))
                .andExpect(jsonPath("$.data.entries[4].path").value("/analytics"))
                .andExpect(jsonPath("$.data.entries[4].badge").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedSummaryIncludesIdentityUnreadCountAndNotifications() throws Exception {
        insertNotification(101L, 2L, "WELCOME", "Older", "Older notification", 0, LocalDateTime.now().minusHours(2));
        insertNotification(102L, 2L, "WELCOME", "Newest", "Newest notification", 1, LocalDateTime.now().minusHours(1));

        mockMvc.perform(get("/api/home/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.viewerType").value("USER"))
                .andExpect(jsonPath("$.data.identity.userId").value(2))
                .andExpect(jsonPath("$.data.identity.phone").value("13800000001"))
                .andExpect(jsonPath("$.data.unreadNotificationCount").value(1))
                .andExpect(jsonPath("$.data.discoverPreview.period").value("WEEK"))
                .andExpect(jsonPath("$.data.entries[2].code").value("resources"))
                .andExpect(jsonPath("$.data.entries[2].badge").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.entries[3].code").value("assessment"))
                .andExpect(jsonPath("$.data.entries[3].enabled").value(true))
                .andExpect(jsonPath("$.data.entries[3].badge").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.entries[4].code").value("analytics"))
                .andExpect(jsonPath("$.data.entries[4].badge").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.latestNotifications[0].title").value("Newest"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminSummaryIncludesAdminEntryCard() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO t_verification_application (id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, NULL, NULL, NULL, ?, ?)",
                201L, 2L, "Normal User", "20260009", "PENDING", LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(1));

        mockMvc.perform(get("/api/home/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.viewerType").value("ADMIN"))
                .andExpect(jsonPath("$.data.entries[5].code").value("admin-dashboard"))
                .andExpect(jsonPath("$.data.entries[5].title").value("Admin Dashboard"))
                .andExpect(jsonPath("$.data.entries[5].path").value("/admin/dashboard"))
                .andExpect(jsonPath("$.data.entries[5].enabled").value(true))
                .andExpect(jsonPath("$.data.entries[5].badge").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.entries[6].code").value("admin-users"))
                .andExpect(jsonPath("$.data.entries[6].path").value("/admin/users"))
                .andExpect(jsonPath("$.data.entries[7].code").value("admin-verifications"))
                .andExpect(jsonPath("$.data.entries[7].path").value("/admin/verifications"));
    }

    private void insertNotification(Long id, Long userId, String type, String title, String content, int isRead,
            LocalDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO t_notification (id, user_id, type, title, content, is_read, source_type, source_id, created_at, read_at) VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?)",
                id, userId, type, title, content, isRead, createdAt, isRead == 1 ? createdAt.plusMinutes(10) : null);
    }
}
