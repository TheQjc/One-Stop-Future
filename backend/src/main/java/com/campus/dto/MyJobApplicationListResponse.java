package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.campus.common.ResourcePreviewKind;

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
            boolean previewAvailable,
            ResourcePreviewKind previewKind,
            LocalDateTime submittedAt) {
    }
}
