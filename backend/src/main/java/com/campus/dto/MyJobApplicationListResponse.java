package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyJobApplicationListResponse(
        int total,
        List<ApplicationItem> applications) {

    public record ApplicationItem(
            Long id,
            Long jobId,
            String jobTitle,
            String companyName,
            String city,
            String status,
            String resumeTitleSnapshot,
            String resumeFileNameSnapshot,
            LocalDateTime submittedAt) {
    }
}
