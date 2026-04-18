package com.campus.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.dto.AnalyticsDistributionRow;
import com.campus.dto.AnalyticsSummaryResponse;
import com.campus.dto.AnalyticsTrendRow;
import com.campus.mapper.AnalyticsReadMapper;

@Service
public class AnalyticsService {

    private final AnalyticsReadMapper analyticsReadMapper;

    public AnalyticsService(AnalyticsReadMapper analyticsReadMapper) {
        this.analyticsReadMapper = analyticsReadMapper;
    }

    public AnalyticsSummaryResponse summary(String viewerIdentity, String period) {
        String normalizedPeriod = normalizePeriod(period);
        int days = "7D".equals(normalizedPeriod) ? 7 : 30;

        AnalyticsSummaryResponse.PublicOverview overview = new AnalyticsSummaryResponse.PublicOverview(
                analyticsReadMapper.countPublishedPosts(),
                analyticsReadMapper.countPublishedJobs(),
                analyticsReadMapper.countPublishedResources(),
                analyticsReadMapper.countAssessmentSessions());

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDateTime startDateTime = startDate.atStartOfDay();

        Map<LocalDate, Integer> postCounts = toDailyCountMap(analyticsReadMapper.summarizePublishedPostTrend(startDateTime));
        Map<LocalDate, Integer> jobCounts = toDailyCountMap(analyticsReadMapper.summarizePublishedJobTrend(startDateTime));
        Map<LocalDate, Integer> resourceCounts = toDailyCountMap(
                analyticsReadMapper.summarizePublishedResourceTrend(startDateTime));
        Map<LocalDate, Integer> assessmentCounts = toDailyCountMap(
                analyticsReadMapper.summarizeAssessmentTrend(startDate));

        List<AnalyticsSummaryResponse.TrendPoint> publicTrends = java.util.stream.IntStream.range(0, days)
                .mapToObj(offset -> {
                    LocalDate date = startDate.plusDays(offset);
                    return new AnalyticsSummaryResponse.TrendPoint(
                            date,
                            postCounts.getOrDefault(date, 0),
                            jobCounts.getOrDefault(date, 0),
                            resourceCounts.getOrDefault(date, 0),
                            assessmentCounts.getOrDefault(date, 0));
                })
                .toList();

        AnalyticsSummaryResponse.DecisionDistribution distribution = computeDistribution(
                analyticsReadMapper.summarizeLatestAssessmentDistribution());

        return new AnalyticsSummaryResponse(
                overview,
                publicTrends,
                distribution,
                "ANONYMOUS",
                null,
                null,
                List.of(),
                List.of());
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return "30D";
        }
        String normalized = period.trim().toUpperCase(java.util.Locale.ROOT);
        if (!"7D".equals(normalized) && !"30D".equals(normalized)) {
            throw new BusinessException(400, "invalid period");
        }
        return normalized;
    }

    private Map<LocalDate, Integer> toDailyCountMap(List<AnalyticsTrendRow> rows) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (AnalyticsTrendRow row : rows) {
            if (row == null || row.bucketDate() == null) {
                continue;
            }
            map.put(row.bucketDate(), row.total());
        }
        return map;
    }

    private AnalyticsSummaryResponse.DecisionDistribution computeDistribution(List<AnalyticsDistributionRow> rows) {
        Map<String, Integer> counts = new HashMap<>();
        for (AnalyticsDistributionRow row : rows) {
            if (row == null || row.track() == null) {
                continue;
            }
            counts.put(row.track(), row.count());
        }

        List<String> orderedTracks = List.of("CAREER", "EXAM", "ABROAD");
        int participantCount = orderedTracks.stream().mapToInt(track -> counts.getOrDefault(track, 0)).sum();
        List<AnalyticsSummaryResponse.TrackMixItem> items = orderedTracks.stream()
                .map(track -> {
                    int count = counts.getOrDefault(track, 0);
                    double percent = participantCount == 0 ? 0.0 : round1(count * 100.0 / participantCount);
                    return new AnalyticsSummaryResponse.TrackMixItem(track, count, percent);
                })
                .toList();

        return new AnalyticsSummaryResponse.DecisionDistribution(participantCount, items);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
