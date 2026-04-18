package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import com.campus.common.BusinessException;
import com.campus.dto.DecisionSchoolCompareRequest;
import com.campus.dto.DecisionSchoolCompareResponse;
import com.campus.dto.DecisionSchoolListResponse;

@SpringBootTest
@Sql(scripts = { "/schema.sql", "/data.sql" }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DecisionSchoolServiceTests {

    @Autowired
    private DecisionSchoolService service;

    @Test
    void listSupportsGuestAndReturnsExamSchoolsInDeterministicOrder() {
        DecisionSchoolListResponse response = service.listSchools("EXAM", null);

        assertThat(response.track()).isEqualTo("EXAM");
        assertThat(response.total()).isEqualTo(3);
        assertThat(response.schools().get(0).schoolId()).isEqualTo(5001L);
    }

    @Test
    void comparePreservesRequestOrderAndMetricOrderAndChartableFilter() {
        DecisionSchoolCompareResponse response = service.compare(new DecisionSchoolCompareRequest(List.of(5002L, 5001L)));

        assertThat(response.schools().get(0).schoolId()).isEqualTo(5002L);
        assertThat(response.schools().get(1).schoolId()).isEqualTo(5001L);

        assertThat(response.metricDefinitions()).isNotEmpty();
        assertThat(response.metricDefinitions().get(0).metricOrder()).isEqualTo(1);
        assertThat(response.tableRows().get(0).metricCode()).isEqualTo(response.metricDefinitions().get(0).metricCode());

        // NOTES is non-chartable in seed; must not appear in chartSeries.
        assertThat(response.chartSeries().stream().anyMatch(series -> "NOTES".equals(series.metricCode()))).isFalse();
    }

    @Test
    void compareRejectsMixedTrack() {
        assertThatThrownBy(() -> service.compare(new DecisionSchoolCompareRequest(List.of(5001L, 6001L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }

    @Test
    void compareRejectsUnknownAndNullSchoolIds() {
        assertThatThrownBy(() -> service.compare(new DecisionSchoolCompareRequest(List.of(5001L, 9999L))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));

        assertThatThrownBy(() -> service.compare(new DecisionSchoolCompareRequest(Arrays.asList(5001L, null))))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(400));
    }
}
