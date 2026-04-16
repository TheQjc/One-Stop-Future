package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminJobListResponse(
        int total,
        List<JobItem> jobs) {

    public record JobItem(
            Long id,
            String title,
            String companyName,
            String city,
            String jobType,
            String educationRequirement,
            String sourcePlatform,
            String status,
            String summary,
            LocalDateTime deadlineAt,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt) {
    }
}
