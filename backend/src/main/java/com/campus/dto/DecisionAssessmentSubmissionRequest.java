package com.campus.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record DecisionAssessmentSubmissionRequest(@NotNull List<AnswerItem> answers) {

    public record AnswerItem(
            @NotNull Long questionId,
            @NotNull Long optionId) {
    }
}

