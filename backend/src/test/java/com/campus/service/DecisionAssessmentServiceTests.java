package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.campus.common.BusinessException;
import com.campus.dto.DecisionAssessmentResultResponse;
import com.campus.dto.DecisionAssessmentSubmissionRequest;
import com.campus.dto.DecisionAssessmentSubmissionRequest.AnswerItem;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DecisionAssessmentServiceTests {

    @Autowired
    private DecisionAssessmentService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void latestWithoutSessionReturnsEmptyResult() {
        DecisionAssessmentResultResponse response = service.latestFor("2");

        assertThat(response.hasResult()).isFalse();
        assertThat(response.recommendedTrack()).isNull();
    }

    @Test
    void submitAnswersPersistsLatestSessionAndReturnsRecommendedTrack() {
        DecisionAssessmentResultResponse response = service.submit("2", new DecisionAssessmentSubmissionRequest(List.of(
                new AnswerItem(1L, 11L),
                new AnswerItem(2L, 22L),
                new AnswerItem(3L, 31L),
                new AnswerItem(4L, 41L),
                new AnswerItem(5L, 51L),
                new AnswerItem(6L, 61L))));

        assertThat(response.hasResult()).isTrue();
        assertThat(response.recommendedTrack()).isEqualTo("EXAM");
        assertThat(response.ranking().get(0).track()).isEqualTo("EXAM");
        assertThat(response.sessionDate()).isEqualTo(LocalDate.now());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_decision_assessment_session WHERE user_id = 2", Integer.class);
        assertThat(count).isEqualTo(1);

        DecisionAssessmentResultResponse latest = service.latestFor("2");
        assertThat(latest.hasResult()).isTrue();
        assertThat(latest.recommendedTrack()).isEqualTo("EXAM");
    }

    @Test
    void submitRejectsOptionQuestionMismatch() {
        assertThatThrownBy(() -> service.submit("2", new DecisionAssessmentSubmissionRequest(List.of(
                new AnswerItem(1L, 22L),
                new AnswerItem(2L, 11L),
                new AnswerItem(3L, 31L),
                new AnswerItem(4L, 41L),
                new AnswerItem(5L, 51L),
                new AnswerItem(6L, 61L)))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }
}

