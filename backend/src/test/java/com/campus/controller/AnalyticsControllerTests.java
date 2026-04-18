package com.campus.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AnalyticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void guestCanReadAnalyticsSummary() throws Exception {
        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.publicOverview.publishedPostCount").isNumber())
                .andExpect(jsonPath("$.data.publicTrends.length()").value(30))
                .andExpect(jsonPath("$.data.decisionDistribution.tracks.length()").value(3))
                .andExpect(jsonPath("$.data.personalSnapshot").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void authenticatedUserReceivesPersonalSnapshotHistoryAndNextActions() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO t_decision_assessment_session (
                  id, user_id, recommended_track, career_score, exam_score, abroad_score, summary_text, session_date, created_at, updated_at
                ) VALUES (3101, 2, 'EXAM', 2, 8, 1, 'seed', DATE '2026-04-18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        mockMvc.perform(get("/api/analytics/summary").param("period", "7D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.personalStatus").value("READY"))
                .andExpect(jsonPath("$.data.personalSnapshot.hasAssessment").value(true))
                .andExpect(jsonPath("$.data.personalSnapshot.recommendedTrack").value("EXAM"))
                .andExpect(jsonPath("$.data.personalHistory.length()").value(1))
                .andExpect(jsonPath("$.data.nextActions.length()").isNumber());
    }

    @Test
    void invalidPeriodReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").param("period", "90D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid period"));
    }
}
