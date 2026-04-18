package com.campus.dto;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsSummaryResponse(
        PublicOverview publicOverview,
        List<TrendPoint> publicTrends,
        DecisionDistribution decisionDistribution,
        String personalStatus,
        String personalMessage,
        PersonalSnapshot personalSnapshot,
        List<PersonalHistoryItem> personalHistory,
        List<NextActionItem> nextActions) {

    public record PublicOverview(
            int publishedPostCount,
            int activeJobCount,
            int publishedResourceCount,
            int assessmentSessionCount) {
    }

    public record TrendPoint(
            LocalDate date,
            int posts,
            int jobs,
            int resources,
            int assessments) {
    }

    public record DecisionDistribution(int participantCount, List<TrackMixItem> tracks) {
    }

    public record TrackMixItem(String track, int count, double percent) {
    }

    public record PersonalSnapshot(
            boolean hasAssessment,
            String recommendedTrack,
            String summaryText,
            LocalDate sessionDate,
            ScoreBundle scores) {
    }

    public record ScoreBundle(int career, int exam, int abroad) {
    }

    public record PersonalHistoryItem(
            LocalDate sessionDate,
            String recommendedTrack,
            int careerScore,
            int examScore,
            int abroadScore) {
    }

    public record NextActionItem(String code, String label, String path, String description) {
    }
}
