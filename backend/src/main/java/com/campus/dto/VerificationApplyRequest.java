package com.campus.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationApplyRequest(
        @NotBlank String realName,
        @NotBlank String studentId) {
}
