package com.campus.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AdminJobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void normalUserCannotOpenAdminJobsList() throws Exception {
        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminListReturnsAllJobs() throws Exception {
        mockMvc.perform(get("/api/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.jobs[0].status").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminCanCreateUpdatePublishAndOfflineJob() throws Exception {
        mockMvc.perform(post("/api/admin/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Data Analyst Trainee",
                                  "companyName":"South Coast Studio",
                                  "city":"Hangzhou",
                                  "jobType":"FULL_TIME",
                                  "educationRequirement":"BACHELOR",
                                  "sourcePlatform":"Official Site",
                                  "sourceUrl":"https://jobs.example.com/south-coast-studio/data-analyst-trainee",
                                  "summary":"Support dashboard delivery, reporting, and operational analysis.",
                                  "content":"Assist with dashboard updates, reporting QA, and weekly hiring analytics.",
                                  "deadlineAt":"2026-06-20T18:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("Data Analyst Trainee"));

        Long createdId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM t_job_posting", Long.class);

        mockMvc.perform(put("/api/admin/jobs/{id}", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Data Analyst Trainee",
                                  "companyName":"South Coast Studio",
                                  "city":"Shanghai",
                                  "jobType":"FULL_TIME",
                                  "educationRequirement":"BACHELOR",
                                  "sourcePlatform":"Official Site",
                                  "sourceUrl":"https://jobs.example.com/south-coast-studio/data-analyst-trainee",
                                  "summary":"Support dashboard delivery, reporting, and operational analysis.",
                                  "content":"Assist with dashboard updates, reporting QA, and weekly hiring analytics.",
                                  "deadlineAt":"2026-06-20T18:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Shanghai"));

        mockMvc.perform(post("/api/admin/jobs/{id}/publish", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        mockMvc.perform(post("/api/admin/jobs/{id}/offline", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("OFFLINE"));

        mockMvc.perform(get("/api/jobs/{id}", createdId).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void deleteJobMarksRowAsDeleted() throws Exception {
        mockMvc.perform(post("/api/admin/jobs/2/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM t_job_posting WHERE id = 2", String.class);
        assertThat(statusValue).isEqualTo("DELETED");
    }
}
