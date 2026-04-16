package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminResourceListResponse(
        int total,
        List<ResourceItem> resources) {

    public record ResourceItem(
            Long id,
            String title,
            String category,
            String uploaderNickname,
            String fileName,
            Long fileSize,
            Integer downloadCount,
            String status,
            String rejectReason,
            LocalDateTime createdAt,
            LocalDateTime reviewedAt,
            LocalDateTime publishedAt) {
    }
}
