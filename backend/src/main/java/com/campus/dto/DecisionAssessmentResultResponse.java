package com.campus.dto;

import java.time.LocalDate;
import java.util.List;

public record DecisionAssessmentResultResponse(
        boolean hasResult,
        String recommendedTrack,
        String summaryText,
        ScoreBundle scores,
        List<RankItem> ranking,
        LocalDate sessionDate,
        List<NextActionItem> nextActions) {

    public record ScoreBundle(int career, int exam, int abroad) {
    }

    public record RankItem(String track, int score) {
    }

    public record NextActionItem(String code, String label, String path) {
    }
}

