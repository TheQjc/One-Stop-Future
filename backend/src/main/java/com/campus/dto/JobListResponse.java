package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record JobListResponse(
        String keyword,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        int total,
        List<JobSummary> jobs) {

    public record JobSummary(
            Long id,
            String title,
            String companyName,
            String city,
            String jobType,
            String educationRequirement,
            String sourcePlatform,
            String summary,
            String status,
            LocalDateTime deadlineAt,
            LocalDateTime publishedAt,
            boolean favoritedByMe) {
    }
}
