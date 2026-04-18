package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.campus.dto.AnalyticsSummaryResponse;
import com.campus.mapper.AnalyticsReadMapper;
import com.campus.mapper.DecisionAssessmentSessionMapper;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AnalyticsServiceTests {

    @Autowired
    private AnalyticsService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void summaryReturnsZeroFilledTrendSeriesAndExplicitTrackMix() {
        AnalyticsSummaryResponse response = service.summary(null, "7D");

        assertThat(response.publicTrends()).hasSize(7);
        assertThat(response.decisionDistribution().tracks()).extracting(AnalyticsSummaryResponse.TrackMixItem::track)
                .containsExactly("CAREER", "EXAM", "ABROAD");
        assertThat(response.personalSnapshot()).isNull();
        assertThat(response.personalHistory()).isEmpty();
        assertThat(response.nextActions()).isEmpty();
        assertThat(response.personalStatus()).isEqualTo("ANONYMOUS");
        assertThat(response.personalMessage()).isNull();
    }

    @Test
    void authenticatedUserWithoutAssessmentsGetsGuidedEmptyPersonalState() {
        AnalyticsSummaryResponse response = service.summary("2", "30D");

        assertThat(response.personalStatus()).isEqualTo("EMPTY");
        assertThat(response.personalMessage()).isNull();
        assertThat(response.personalSnapshot()).isNotNull();
        assertThat(response.personalSnapshot().hasAssessment()).isFalse();
        assertThat(response.personalHistory()).isEmpty();
        assertThat(response.nextActions()).hasSize(1);
        assertThat(response.nextActions().get(0).code()).isEqualTo("START_ASSESSMENT");
        assertThat(response.nextActions().get(0).path()).isEqualTo("/assessment");
    }

    @Test
    void authenticatedUserWithAssessmentsGetsReadyPersonalState() {
        jdbcTemplate.update("""
                INSERT INTO t_decision_assessment_session (
                  id, user_id, recommended_track, career_score, exam_score, abroad_score, summary_text, session_date, created_at, updated_at
                ) VALUES (3001, 2, 'EXAM', 5, 12, 1, 'seed latest', DATE '2026-04-18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO t_decision_assessment_session (
                  id, user_id, recommended_track, career_score, exam_score, abroad_score, summary_text, session_date, created_at, updated_at
                ) VALUES (3000, 2, 'CAREER', 10, 2, 0, 'seed older', DATE '2026-04-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        AnalyticsSummaryResponse response = service.summary("2", "7D");

        assertThat(response.personalStatus()).isEqualTo("READY");
        assertThat(response.personalMessage()).isNull();
        assertThat(response.personalSnapshot()).isNotNull();
        assertThat(response.personalSnapshot().hasAssessment()).isTrue();
        assertThat(response.personalSnapshot().recommendedTrack()).isEqualTo("EXAM");
        assertThat(response.personalHistory()).hasSize(2);
        assertThat(response.nextActions()).extracting(AnalyticsSummaryResponse.NextActionItem::code)
                .contains("START_ASSESSMENT", "OPEN_TIMELINE", "COMPARE_SCHOOLS");
    }

    @Test
    void authenticatedPersonalFailureDoesNotBreakPublicSummary() {
        AnalyticsReadMapper analyticsReadMapper = mock(AnalyticsReadMapper.class);
        when(analyticsReadMapper.countPublishedPosts()).thenReturn(0);
        when(analyticsReadMapper.countPublishedJobs()).thenReturn(0);
        when(analyticsReadMapper.countPublishedResources()).thenReturn(0);
        when(analyticsReadMapper.countAssessmentSessions()).thenReturn(0);
        when(analyticsReadMapper.summarizePublishedPostTrend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(analyticsReadMapper.summarizePublishedJobTrend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(analyticsReadMapper.summarizePublishedResourceTrend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(analyticsReadMapper.summarizeAssessmentTrend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(analyticsReadMapper.summarizeLatestAssessmentDistribution()).thenReturn(List.of());

        DecisionAssessmentSessionMapper failingSessionMapper = mock(DecisionAssessmentSessionMapper.class);
        when(failingSessionMapper.selectList(org.mockito.ArgumentMatchers.any())).thenThrow(new RuntimeException("boom"));

        AnalyticsService failingService = new AnalyticsService(analyticsReadMapper, failingSessionMapper);

        AnalyticsSummaryResponse response = failingService.summary("2", "7D");

        assertThat(response.publicTrends()).hasSize(7);
        assertThat(response.personalStatus()).isEqualTo("ERROR");
        assertThat(response.personalMessage()).isEqualTo("Personal analytics temporarily unavailable.");
        assertThat(response.personalSnapshot()).isNull();
        assertThat(response.personalHistory()).isEmpty();
        assertThat(response.nextActions()).isEmpty();
    }
}
