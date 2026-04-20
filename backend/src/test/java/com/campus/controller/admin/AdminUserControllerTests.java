package com.campus.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class AdminUserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotOpenAdminUsersList() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminListReturnsUsersAndCounts() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.activeCount").value(3))
                .andExpect(jsonPath("$.data.bannedCount").value(0))
                .andExpect(jsonPath("$.data.verifiedCount").value(1))
                .andExpect(jsonPath("$.data.users[0].id").value(3))
                .andExpect(jsonPath("$.data.users[0].status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanBanAndUnbanNormalUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/2/ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.status").value("BANNED"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_user WHERE id = 2", String.class)).isEqualTo("BANNED");

        mockMvc.perform(post("/api/admin/users/2/unban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM t_user WHERE id = 2", String.class)).isEqualTo("ACTIVE");
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminAccountStatusCannotBeChanged() throws Exception {
        mockMvc.perform(post("/api/admin/users/1/ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("admin account status cannot be changed"));
    }
}
