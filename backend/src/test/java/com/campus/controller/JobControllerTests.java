package com.campus.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class JobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guestCanReadPublishedJobsAndFilterByCity() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.jobs[0].title").isNotEmpty());

        mockMvc.perform(get("/api/jobs").param("city", "Shenzhen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.city").value("Shenzhen"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.jobs[0].city").value("Shenzhen"));
    }

    @Test
    void guestCanReadPublishedJobDetailOnly() throws Exception {
        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Java Backend Intern"))
                .andExpect(jsonPath("$.data.sourceUrl").value("https://jobs.example.com/future-campus-tech/backend-intern"));

        mockMvc.perform(get("/api/jobs/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("job not found"));
    }

    @Test
    void guestCannotFavoriteJob() throws Exception {
        mockMvc.perform(post("/api/jobs/1/favorite"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserCanFavoriteAndUnfavoriteJobIdempotently() throws Exception {
        mockMvc.perform(post("/api/jobs/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true));

        mockMvc.perform(post("/api/jobs/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true));

        Integer favoriteCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user_favorite WHERE user_id = 2 AND target_type = 'JOB' AND target_id = 1",
                Integer.class);
        assertThat(favoriteCount).isEqualTo(1);

        mockMvc.perform(delete("/api/jobs/1/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.favoritedByMe").value(false));
    }

    @Test
    void invalidEnumFilterReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/jobs").param("jobType", "PART_TIME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid job type"));
    }
}
