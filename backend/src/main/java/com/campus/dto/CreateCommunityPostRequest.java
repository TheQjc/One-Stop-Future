package com.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityPostRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String tag,
        @NotBlank @Size(max = 10000) String content,
        Boolean experiencePost,
        String experienceTargetLabel,
        String experienceOutcomeLabel,
        String experienceTimelineSummary,
        String experienceActionSummary) {
}
