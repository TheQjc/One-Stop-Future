package com.campus.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
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
class AdminResourceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .andExpect(jsonPath("$.data.resources[0].status").isNotEmpty());
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
                .andExpect(jsonPath("$.data.resources[1].id").value(3))
                .andExpect(jsonPath("$.data.resources[1].previewAvailable").value(false));
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
}
