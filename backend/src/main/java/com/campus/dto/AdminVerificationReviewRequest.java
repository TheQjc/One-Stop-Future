package com.campus.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminVerificationReviewRequest(
        @NotBlank String action,
        String reason) {
}
