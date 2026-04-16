package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyResourceListResponse(
        int total,
        List<ResourceItem> resources) {

    public record ResourceItem(
            Long id,
            String title,
            String category,
            String summary,
            String status,
            String fileName,
            Long fileSize,
            String rejectReason,
            LocalDateTime createdAt,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt) {
    }
}
