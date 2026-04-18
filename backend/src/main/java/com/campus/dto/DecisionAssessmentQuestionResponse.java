package com.campus.dto;

import java.util.List;

public record DecisionAssessmentQuestionResponse(List<QuestionItem> questions) {

    public record QuestionItem(Long id, String code, String prompt, String description, int displayOrder,
            List<OptionItem> options) {
    }

    public record OptionItem(Long id, String code, String label, String description, int displayOrder) {
    }
}

