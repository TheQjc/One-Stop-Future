package com.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationApplyRequest(
        @NotBlank @Size(max = 50, message = "real name must be at most 50 characters") String realName,
        @NotBlank @Size(max = 50, message = "student ID must be at most 50 characters") String studentId) {
}
