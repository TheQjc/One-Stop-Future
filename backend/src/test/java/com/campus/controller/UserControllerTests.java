package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void meEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void getProfileReturnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.phone").value("13800000001"))
                .andExpect(jsonPath("$.data.nickname").value("NormalUser"))
                .andExpect(jsonPath("$.data.verificationStatus").value("UNVERIFIED"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void updateProfileReturnsUpdatedFields() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"FutureRunner","realName":"Student Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickname").value("FutureRunner"))
                .andExpect(jsonPath("$.data.realName").value("Student Updated"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void postFavoritesReturnPublishedCommunityPosts() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO t_user_favorite (id, user_id, target_type, target_id, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                101L, 2L, "POST", 2L);

        mockMvc.perform(get("/api/users/me/favorites").param("type", "POST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.posts[0].title").value("Exam planning checklist"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void jobFavoritesReturnPublishedJobs() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO t_user_favorite (id, user_id, target_type, target_id, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                102L, 2L, "JOB", 1L);

        mockMvc.perform(get("/api/users/me/favorites").param("type", "JOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.jobs[0].title").value("Java Backend Intern"));
    }
}
