package com.campus.dto;

import jakarta.validation.constraints.NotNull;

public record ApplyJobRequest(
        @NotNull(message = "resume is required") Long resumeId) {
}
