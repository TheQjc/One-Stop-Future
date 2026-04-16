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
class AdminCommunityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotOpenAdminCommunityList() throws Exception {
        mockMvc.perform(get("/api/admin/community/posts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminListReturnsAllPosts() throws Exception {
        mockMvc.perform(get("/api/admin/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.posts[0].status").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void hidePostMakesItUnavailableToPublicDetail() throws Exception {
        mockMvc.perform(post("/api/admin/community/posts/1/hide"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM t_community_post WHERE id = 1", String.class);
        assertThat(statusValue).isEqualTo("HIDDEN");

        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void deletePostMarksRowAsDeleted() throws Exception {
        mockMvc.perform(post("/api/admin/community/posts/2/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM t_community_post WHERE id = 2", String.class);
        assertThat(statusValue).isEqualTo("DELETED");
    }
}
