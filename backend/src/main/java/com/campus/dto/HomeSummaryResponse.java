package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HomeSummaryResponse(
        String viewerType,
        IdentitySnapshot identity,
        String roleLabel,
        String verificationStatus,
        int unreadNotificationCount,
        List<String> todos,
        List<HomeEntryCard> entries,
        List<NotificationSnippet> latestNotifications) {

    public record IdentitySnapshot(
            Long userId,
            String phone,
            String nickname,
            String role,
            String verificationStatus) {
    }

    public record HomeEntryCard(
            String code,
            String title,
            String path,
            boolean enabled,
            String badge) {
    }

    public record NotificationSnippet(
            Long id,
            String type,
            String title,
            String content,
            boolean read,
            LocalDateTime createdAt) {
    }
}
