package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.campus.common.BusinessException;
import com.campus.dto.DecisionTimelineResponse;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DecisionTimelineServiceTests {

    @Autowired
    private DecisionTimelineService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void timelineUsesAnchorDateToProjectTargetDateAndRemainingDays() {
        LocalDate anchor = LocalDate.parse("2026-05-01");
        DecisionTimelineResponse response = service.timelineFor("2", "EXAM", anchor);

        assertThat(response.assessmentRequired()).isFalse();
        assertThat(response.anchorDate()).isEqualTo(anchor);
        assertThat(response.items()).isNotEmpty();

        LocalDate expectedTargetDate = anchor; // seeded EXAM_P0 uses 0 offsets
        assertThat(response.items().get(0).targetDate()).isEqualTo(expectedTargetDate);
        assertThat(response.items().get(0).remainingDays()).isEqualTo(ChronoUnit.DAYS.between(LocalDate.now(), expectedTargetDate));
        assertThat(response.items().get(0).actionChecklist()).isNotEmpty();
    }

    @Test
    void timelineReturnsAssessmentRequiredWhenNoLatestResultOrAnchorExists() {
        DecisionTimelineResponse response = service.timelineFor("2", "EXAM", null);

        assertThat(response.assessmentRequired()).isTrue();
        assertThat(response.items()).isEmpty();
        assertThat(response.anchorDate()).isNull();
    }

    @Test
    void timelineUsesLatestAssessmentSessionDateWhenAnchorMissing() {
        jdbcTemplate.update("""
                INSERT INTO t_decision_assessment_session (
                  id, user_id, recommended_track, career_score, exam_score, abroad_score, summary_text, session_date, created_at, updated_at
                ) VALUES (2001, 2, 'EXAM', 1, 10, 0, 'seed', DATE '2026-06-01', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        DecisionTimelineResponse response = service.timelineFor("2", "EXAM", null);

        assertThat(response.assessmentRequired()).isFalse();
        assertThat(response.anchorDate()).isEqualTo(LocalDate.parse("2026-06-01"));
        assertThat(response.items()).isNotEmpty();
    }

    @Test
    void timelineRejectsInvalidTrack() {
        assertThatThrownBy(() -> service.timelineFor("2", "UNKNOWN", LocalDate.parse("2026-05-01")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }
}

