package com.campus.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.dto.AnalyticsSummaryResponse.NextActionItem;
import com.campus.dto.AnalyticsSummaryResponse.PersonalHistoryItem;
import com.campus.dto.AnalyticsSummaryResponse.PersonalSnapshot;
import com.campus.dto.AnalyticsSummaryResponse.ScoreBundle;
import com.campus.dto.AnalyticsDistributionRow;
import com.campus.dto.AnalyticsSummaryResponse;
import com.campus.dto.AnalyticsTrendRow;
import com.campus.entity.DecisionAssessmentSession;
import com.campus.mapper.AnalyticsReadMapper;
import com.campus.mapper.DecisionAssessmentSessionMapper;

@Service
public class AnalyticsService {

    private final AnalyticsReadMapper analyticsReadMapper;
    private final DecisionAssessmentSessionMapper decisionAssessmentSessionMapper;

    public AnalyticsService(AnalyticsReadMapper analyticsReadMapper,
            DecisionAssessmentSessionMapper decisionAssessmentSessionMapper) {
        this.analyticsReadMapper = analyticsReadMapper;
        this.decisionAssessmentSessionMapper = decisionAssessmentSessionMapper;
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

        if (viewerIdentity == null || viewerIdentity.isBlank() || "anonymousUser".equals(viewerIdentity)) {
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

        try {
            return withPersonalSlice(overview, publicTrends, distribution, viewerIdentity);
        } catch (RuntimeException ex) {
            return new AnalyticsSummaryResponse(
                    overview,
                    publicTrends,
                    distribution,
                    "ERROR",
                    "Personal analytics temporarily unavailable.",
                    null,
                    List.of(),
                    List.of());
        }
    }

    private AnalyticsSummaryResponse withPersonalSlice(
            AnalyticsSummaryResponse.PublicOverview overview,
            List<AnalyticsSummaryResponse.TrendPoint> publicTrends,
            AnalyticsSummaryResponse.DecisionDistribution distribution,
            String viewerIdentity) {
        Long userId = requireUserId(viewerIdentity);
        List<DecisionAssessmentSession> sessions = decisionAssessmentSessionMapper.selectList(
                new LambdaQueryWrapper<DecisionAssessmentSession>()
                        .eq(DecisionAssessmentSession::getUserId, userId)
                        .orderByDesc(DecisionAssessmentSession::getSessionDate)
                        .orderByDesc(DecisionAssessmentSession::getId)
                        .last("LIMIT 5"));

        if (sessions == null || sessions.isEmpty()) {
            PersonalSnapshot snapshot = new PersonalSnapshot(false, null, null, null, null);
            return new AnalyticsSummaryResponse(
                    overview,
                    publicTrends,
                    distribution,
                    "EMPTY",
                    null,
                    snapshot,
                    List.of(),
                    List.of(startAssessmentAction()));
        }

        DecisionAssessmentSession latest = sessions.get(0);
        ScoreBundle scores = new ScoreBundle(
                safeInt(latest.getCareerScore()),
                safeInt(latest.getExamScore()),
                safeInt(latest.getAbroadScore()));
        PersonalSnapshot snapshot = new PersonalSnapshot(
                true,
                latest.getRecommendedTrack(),
                latest.getSummaryText(),
                latest.getSessionDate(),
                scores);

        List<PersonalHistoryItem> history = sessions.stream()
                .map(session -> new PersonalHistoryItem(
                        session.getSessionDate(),
                        session.getRecommendedTrack(),
                        safeInt(session.getCareerScore()),
                        safeInt(session.getExamScore()),
                        safeInt(session.getAbroadScore())))
                .toList();

        List<NextActionItem> nextActions = nextActionsFor(latest.getRecommendedTrack());

        return new AnalyticsSummaryResponse(
                overview,
                publicTrends,
                distribution,
                "READY",
                null,
                snapshot,
                history,
                nextActions);
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

    private Long requireUserId(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new BusinessException(401, "unauthorized");
        }
        if (!identity.matches("^\\d+$")) {
            throw new BusinessException(401, "unauthorized");
        }
        try {
            return Long.parseLong(identity);
        } catch (NumberFormatException ex) {
            throw new BusinessException(401, "unauthorized");
        }
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

    private List<NextActionItem> nextActionsFor(String recommendedTrack) {
        List<NextActionItem> items = new java.util.ArrayList<>();
        items.add(startAssessmentAction());
        items.add(new NextActionItem(
                "OPEN_TIMELINE",
                "Open Timeline",
                "/timeline",
                "Turn your latest recommendation into an actionable plan."));

        if ("EXAM".equalsIgnoreCase(recommendedTrack) || "ABROAD".equalsIgnoreCase(recommendedTrack)) {
            items.add(new NextActionItem(
                    "COMPARE_SCHOOLS",
                    "Compare Schools",
                    "/schools/compare",
                    "Compare key options side-by-side for your track."));
        }
        return items;
    }

    private NextActionItem startAssessmentAction() {
        return new NextActionItem(
                "START_ASSESSMENT",
                "Start Assessment",
                "/assessment",
                "Answer a short assessment to generate or refresh your direction snapshot.");
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
