package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationListResponse(
        int unreadCount,
        int total,
        List<NotificationItem> notifications) {

    public record NotificationItem(
            Long id,
            String type,
            String title,
            String content,
            boolean read,
            LocalDateTime createdAt,
            LocalDateTime readAt) {
    }
}
