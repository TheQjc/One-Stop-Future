package com.campus.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 80) String companyName,
        @NotBlank @Size(max = 80) String city,
        @NotBlank String jobType,
        @NotBlank String educationRequirement,
        @NotBlank @Size(max = 50) String sourcePlatform,
        @NotBlank @Size(max = 500) String sourceUrl,
        @NotBlank @Size(max = 300) String summary,
        @Size(max = 10000) String content,
        LocalDateTime deadlineAt) {
}
