package com.campus.dto;

import java.time.LocalDateTime;

public record JobDetailResponse(
        Long id,
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        String sourceUrl,
        String summary,
        String content,
        String status,
        LocalDateTime deadlineAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean favoritedByMe,
        boolean appliedByMe,
        Long applicationId) {
}
