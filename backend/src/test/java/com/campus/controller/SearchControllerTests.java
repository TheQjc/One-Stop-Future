package com.campus.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SearchControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guestCanUsePublicSearchEndpointForResources() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "resume")
                        .param("type", "RESOURCE")
                        .param("sort", "RELEVANCE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.query").value("resume"))
                .andExpect(jsonPath("$.data.type").value("RESOURCE"))
                .andExpect(jsonPath("$.data.sort").value("RELEVANCE"))
                .andExpect(jsonPath("$.data.totals.resource").value(1))
                .andExpect(jsonPath("$.data.results[0].type").value("RESOURCE"))
                .andExpect(jsonPath("$.data.results[0].path").value("/resources/1"));
    }

    @Test
    void defaultsTypeAndSortWhenOptionalParamsMissing() throws Exception {
        mockMvc.perform(get("/api/search").param("q", " resume "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.query").value("resume"))
                .andExpect(jsonPath("$.data.type").value("ALL"))
                .andExpect(jsonPath("$.data.sort").value("RELEVANCE"))
                .andExpect(jsonPath("$.data.totals.all").value(1))
                .andExpect(jsonPath("$.data.totals.resource").value(1));
    }

    @Test
    void blankQueryReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("search query is required"));
    }

    @Test
    void invalidTypeAndSortReturnBusinessErrors() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "resume").param("type", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid search type"));

        mockMvc.perform(get("/api/search").param("q", "resume").param("sort", "HOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid search sort"));
    }

    @Test
    void allSearchAggregatesPublishedPostJobAndResourceHits() throws Exception {
        insertUnifiedSearchFixtures();

        mockMvc.perform(get("/api/search").param("q", "unified"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totals.all").value(3))
                .andExpect(jsonPath("$.data.totals.post").value(1))
                .andExpect(jsonPath("$.data.totals.job").value(1))
                .andExpect(jsonPath("$.data.totals.resource").value(1))
                .andExpect(jsonPath("$.data.results", hasSize(3)))
                .andExpect(jsonPath("$.data.results[0].type").value("RESOURCE"));
    }

    @Test
    void typeFilterKeepsTotalsButReturnsOnlyMatchingBranch() throws Exception {
        insertUnifiedSearchFixtures();

        mockMvc.perform(get("/api/search").param("q", "unified").param("type", "JOB"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totals.all").value(3))
                .andExpect(jsonPath("$.data.totals.post").value(1))
                .andExpect(jsonPath("$.data.totals.job").value(1))
                .andExpect(jsonPath("$.data.totals.resource").value(1))
                .andExpect(jsonPath("$.data.results", hasSize(1)))
                .andExpect(jsonPath("$.data.results[0].type").value("JOB"))
                .andExpect(jsonPath("$.data.results[0].path").value("/jobs/101"));
    }

    @Test
    void latestSortOrdersCrossDomainResultsByPublishedTimeDescending() throws Exception {
        insertUnifiedSearchFixtures();

        mockMvc.perform(get("/api/search").param("q", "unified").param("sort", "LATEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.results[0].path").value("/resources/101"))
                .andExpect(jsonPath("$.data.results[1].path").value("/jobs/101"))
                .andExpect(jsonPath("$.data.results[2].path").value("/community/101"));
    }

    private void insertUnifiedSearchFixtures() {
        jdbcTemplate.update("""
                INSERT INTO t_community_post (id, author_id, tag, title, content, status, like_count, comment_count, favorite_count, created_at, updated_at)
                VALUES (101, 2, 'CAREER', 'Unified planning note', 'Cross-domain unified content body', 'PUBLISHED', 0, 0, 0, TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -3, CURRENT_TIMESTAMP))
                """);
        jdbcTemplate.update("""
                INSERT INTO t_job_posting (id, title, company_name, city, job_type, education_requirement, source_platform, source_url, summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at)
                VALUES (101, 'Unified campus job', 'Search Labs', 'Shenzhen', 'INTERNSHIP', 'BACHELOR', 'Official Site', 'https://jobs.example.com/unified-job', 'Unified search summary', 'Backend job body', TIMESTAMPADD(DAY, 7, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -2, CURRENT_TIMESTAMP), 'PUBLISHED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO t_resource_item (id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason, file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count, published_at, reviewed_at, created_at, updated_at)
                VALUES (101, 'Unified resume pack', 'RESUME_TEMPLATE', 'Unified search summary', 'Resource body', 'PUBLISHED', 3, 1, NULL, 'unified-pack.pdf', 'pdf', 'application/pdf', 1234, 'seed/unified-pack.pdf', 0, 0, TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), TIMESTAMPADD(DAY, -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
    }

}
