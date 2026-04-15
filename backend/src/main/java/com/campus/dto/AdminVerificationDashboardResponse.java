package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminVerificationDashboardResponse(
        int pendingCount,
        int reviewedToday,
        List<VerificationApplicationSummary> latestPendingApplications) {

    public record VerificationApplicationSummary(
            Long id,
            Long userId,
            String applicantNickname,
            String realName,
            String studentId,
            String status,
            LocalDateTime createdAt) {
    }
}
