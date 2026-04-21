package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DiscoverControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guestCanUsePublicDiscoverEndpointForResources() throws Exception {
        mockMvc.perform(get("/api/discover")
                        .param("tab", "RESOURCE")
                        .param("period", "WEEK")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tab").value("RESOURCE"))
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].type").value("RESOURCE"))
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.items[0].path").value("/resources/1"))
                .andExpect(jsonPath("$.data.items[0].hotLabel").value("\u672c\u5468\u9ad8\u9891\u4e0b\u8f7d"));
    }

    @Test
    void defaultsAndValidationErrorsMatchTheContract() throws Exception {
        mockMvc.perform(get("/api/discover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tab").value("ALL"))
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.total").value(7));

        mockMvc.perform(get("/api/discover").param("tab", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid discover tab"));

        mockMvc.perform(get("/api/discover").param("period", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid discover period"));

        mockMvc.perform(get("/api/discover").param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid discover limit"));
    }

    @Test
    void allTabAggregatesPostJobAndResourceItems() throws Exception {
        replaceDiscoverFixtures();

        mockMvc.perform(get("/api/discover")
                        .param("tab", "ALL")
                        .param("period", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.items[0].path").value("/resources/101"))
                .andExpect(jsonPath("$.data.items[1].path").value("/community/101"))
                .andExpect(jsonPath("$.data.items[2].path").value("/jobs/101"))
                .andExpect(jsonPath("$.data.items[3].path").value("/community/102"));
    }

    @Test
    void weekPeriodOnlyIncludesItemsPublishedInTheLastSevenDays() throws Exception {
        replaceDiscoverFixtures();

        mockMvc.perform(get("/api/discover")
                        .param("tab", "ALL")
                        .param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.items[0].path").value("/resources/101"))
                .andExpect(jsonPath("$.data.items[1].path").value("/community/101"))
                .andExpect(jsonPath("$.data.items[2].path").value("/jobs/101"));
    }

    @Test
    void tabFilterReturnsOnlyMatchingTypeButKeepsSharedItemShape() throws Exception {
        replaceDiscoverFixtures();

        mockMvc.perform(get("/api/discover")
                        .param("tab", "JOB")
                        .param("period", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].type").value("JOB"))
                .andExpect(jsonPath("$.data.items[0].path").value("/jobs/101"))
                .andExpect(jsonPath("$.data.items[0].hotLabel").value("\u6301\u7eed\u5173\u6ce8"));
    }

    @Test
    void experiencePostsReceiveDeterministicDiscoverBonus() throws Exception {
        replaceDiscoverFixtures();
        jdbcTemplate.update("""
                UPDATE t_community_post
                SET author_id = 3,
                    like_count = 2,
                    comment_count = 0,
                    favorite_count = 0,
                    is_experience_post = 0,
                    created_at = TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP),
                    updated_at = TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP)
                WHERE id = 101
                """);
        jdbcTemplate.update("""
                UPDATE t_community_post
                SET author_id = 2,
                    like_count = 1,
                    comment_count = 1,
                    favorite_count = 0,
                    is_experience_post = 1,
                    experience_target_label = 'Interview sprint',
                    experience_outcome_label = 'Reached final round',
                    created_at = TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP),
                    updated_at = TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP)
                WHERE id = 102
                """);

        mockMvc.perform(get("/api/discover")
                        .param("tab", "POST")
                        .param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(102))
                .andExpect(jsonPath("$.data.items[0].secondaryMeta").value("EXAM / Experience Post"))
                .andExpect(jsonPath("$.data.items[0].hotLabel").value("Weekly experience pick"))
                .andExpect(jsonPath("$.data.items[1].id").value(101));
    }

    private void replaceDiscoverFixtures() {
        jdbcTemplate.update("DELETE FROM t_user_favorite");
        jdbcTemplate.update("DELETE FROM t_community_post_like");
        jdbcTemplate.update("DELETE FROM t_community_comment");
        jdbcTemplate.update("DELETE FROM t_resource_item");
        jdbcTemplate.update("DELETE FROM t_job_posting");
        jdbcTemplate.update("DELETE FROM t_community_post");

        jdbcTemplate.update("""
                INSERT INTO t_community_post (
                    id, author_id, tag, title, content, status, like_count, comment_count, favorite_count,
                    is_experience_post, experience_target_label, experience_outcome_label,
                    experience_timeline_summary, experience_action_summary, created_at, updated_at
                )
                VALUES (
                    101, 3, 'CAREER', 'Verified post momentum', 'A verified-user post with stable engagement.', 'PUBLISHED', 2, 1, 1,
                    0, NULL, NULL, NULL, NULL,
                    TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP)
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO t_community_post (
                    id, author_id, tag, title, content, status, like_count, comment_count, favorite_count,
                    is_experience_post, experience_target_label, experience_outcome_label,
                    experience_timeline_summary, experience_action_summary, created_at, updated_at
                )
                VALUES (
                    102, 2, 'EXAM', 'Old but visible planning post', 'An older planning note kept for total ranking only.', 'PUBLISHED', 4, 0, 0,
                    1, 'Exam turnaround', 'Reached shortlist', 'Month 1 diagnose, month 2 drills', 'Use one notebook for mistakes and weekly review',
                    TIMESTAMPADD(DAY, -10, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -10, CURRENT_TIMESTAMP)
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO t_job_posting (id, title, company_name, city, job_type, education_requirement, source_platform, source_url, summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at)
                VALUES (101, 'Discover backend role', 'Campus Discover Labs', 'Shenzhen', 'INTERNSHIP', 'BACHELOR', 'Official Site', 'https://jobs.example.com/discover-role', 'A job card used for discover ranking.', 'Job body', TIMESTAMPADD(DAY, 15, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO t_resource_item (id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason, file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count, published_at, reviewed_at, created_at, updated_at)
                VALUES (101, 'Discover resume resource', 'RESUME_TEMPLATE', 'A strong resource candidate for discover.', 'Resource body', 'PUBLISHED', 3, 1, NULL, 'discover-pack.pdf', 'pdf', 'application/pdf', 1234, 'seed/discover-pack.pdf', 9, 1, TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        jdbcTemplate.update("""
                INSERT INTO t_user_favorite (id, user_id, target_type, target_id, created_at)
                VALUES (201, 1, 'JOB', 101, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO t_user_favorite (id, user_id, target_type, target_id, created_at)
                VALUES (202, 2, 'JOB', 101, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO t_user_favorite (id, user_id, target_type, target_id, created_at)
                VALUES (203, 3, 'JOB', 101, CURRENT_TIMESTAMP)
                """);
    }
}
