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
class DecisionTimelineControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void timelineRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/decision/timeline").param("track", "EXAM"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void invalidTrackReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/decision/timeline").param("track", "NOPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid track"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void trackMissingReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/decision/timeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("track is required"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void timelineReturnsAssessmentRequiredWhenNoLatestResultOrAnchorExists() throws Exception {
        mockMvc.perform(get("/api/decision/timeline").param("track", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assessmentRequired").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void explicitAnchorDateReturnsOrderedMilestones() throws Exception {
        mockMvc.perform(get("/api/decision/timeline")
                        .param("track", "EXAM")
                        .param("anchorDate", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assessmentRequired").value(false))
                .andExpect(jsonPath("$.data.track").value("EXAM"))
                .andExpect(jsonPath("$.data.anchorDate").value("2026-05-01"))
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[0].phaseCode").value("EXAM_P0"))
                .andExpect(jsonPath("$.data.items[0].targetDate").value("2026-05-01"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void invalidAnchorDateReturnsBusinessError() throws Exception {
        mockMvc.perform(get("/api/decision/timeline")
                        .param("track", "EXAM")
                        .param("anchorDate", "2026/05/01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("invalid anchorDate"));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void configuredAnchorWithNoMilestonesReturnsEmptyItems() throws Exception {
        jdbcTemplate.update("DELETE FROM t_decision_timeline_milestone WHERE track = 'EXAM'");

        mockMvc.perform(get("/api/decision/timeline")
                        .param("track", "EXAM")
                        .param("anchorDate", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assessmentRequired").value(false))
                .andExpect(jsonPath("$.data.anchorDate").value("2026-05-01"))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    @WithMockUser(username = "2", roles = "USER")
    void latestSessionAnchorFallbackWorks() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO t_decision_assessment_session (
                  id, user_id, recommended_track, career_score, exam_score, abroad_score, summary_text, session_date, created_at, updated_at
                ) VALUES (3001, 2, 'EXAM', 1, 10, 0, 'seed', DATE '2026-06-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        mockMvc.perform(get("/api/decision/timeline").param("track", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.assessmentRequired").value(false))
                .andExpect(jsonPath("$.data.anchorDate").value("2026-06-01"))
                .andExpect(jsonPath("$.data.items.length()").value(4));
    }
}
