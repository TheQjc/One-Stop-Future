package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import com.campus.dto.AnalyticsSummaryResponse;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AnalyticsServiceTests {

    @Autowired
    private AnalyticsService service;

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
}

