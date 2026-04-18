package com.campus.dto;

import java.util.List;

import com.campus.dto.DecisionSchoolListResponse.SchoolItem;

public record DecisionSchoolCompareResponse(
        List<SchoolItem> schools,
        List<MetricDefinitionItem> metricDefinitions,
        List<TableRowItem> tableRows,
        List<ChartSeriesItem> chartSeries,
        String highlightSummary) {

    public record MetricDefinitionItem(
            String metricCode,
            String metricLabel,
            String metricUnit,
            String valueType,
            boolean chartable,
            int metricOrder) {
    }

    public record TableRowItem(
            String metricCode,
            String metricLabel,
            String metricUnit,
            String valueType,
            List<TableCellItem> cells) {
    }

    public record TableCellItem(
            Long schoolId,
            String displayValue,
            String rawValue,
            boolean isMissing) {
    }

    public record ChartSeriesItem(
            String metricCode,
            String metricLabel,
            String metricUnit,
            String valueType,
            List<ChartPointItem> points) {
    }

    public record ChartPointItem(
            Long schoolId,
            String schoolName,
            Double numericValue,
            String displayValue,
            boolean isMissing) {
    }
}

