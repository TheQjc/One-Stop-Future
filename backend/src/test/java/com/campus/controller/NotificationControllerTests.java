package com.campus.controller;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class NotificationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void listReturnsNotificationsAndUnreadCount() throws Exception {
        insertNotification(101L, 2L, "WELCOME", "Older", "Older notification", 1, LocalDateTime.now().minusHours(2));
        insertNotification(102L, 2L, "VERIFICATION_APPROVED", "Newest", "Newest notification", 0,
                LocalDateTime.now().minusHours(1));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.notifications[0].title").value("Newest"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void markReadMarksSingleNotificationAsRead() throws Exception {
        insertNotification(101L, 2L, "WELCOME", "Unread", "Unread notification", 0, LocalDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/notifications/101/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer isRead = jdbcTemplate.queryForObject("SELECT is_read FROM t_notification WHERE id = 101", Integer.class);
        assertThat(isRead).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void markAllReadMarksOnlyCurrentUsersNotifications() throws Exception {
        insertNotification(101L, 2L, "WELCOME", "Unread 1", "Unread notification", 0, LocalDateTime.now().minusHours(2));
        insertNotification(102L, 2L, "WELCOME", "Unread 2", "Unread notification", 0, LocalDateTime.now().minusHours(1));
        insertNotification(103L, 1L, "WELCOME", "Other user", "Unread notification", 0, LocalDateTime.now().minusHours(1));

        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.updatedCount").value(2));

        Integer currentUserUnread = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = 2 AND is_read = 0", Integer.class);
        Integer otherUserUnread = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_notification WHERE user_id = 1 AND is_read = 0", Integer.class);
        assertThat(currentUserUnread).isEqualTo(0);
        assertThat(otherUserUnread).isEqualTo(1);
    }

    private void insertNotification(Long id, Long userId, String type, String title, String content, int isRead,
            LocalDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO t_notification (id, user_id, type, title, content, is_read, source_type, source_id, created_at, read_at) VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?)",
                id, userId, type, title, content, isRead, createdAt, isRead == 1 ? createdAt.plusMinutes(10) : null);
    }
}
