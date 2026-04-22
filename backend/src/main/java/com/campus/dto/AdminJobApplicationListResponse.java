package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.campus.common.ResourcePreviewKind;

public record AdminJobApplicationListResponse(
        int total,
        int submittedToday,
        int uniqueApplicants,
        int uniqueJobs,
        List<ApplicationItem> applications) {

    public record ApplicationItem(
            Long id,
            Long jobId,
            String jobTitle,
            String companyName,
            Long applicantUserId,
            String applicantNickname,
            String resumeFileNameSnapshot,
            boolean previewAvailable,
            ResourcePreviewKind previewKind,
            String status,
            LocalDateTime submittedAt) {
    }
}
