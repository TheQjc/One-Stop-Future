package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResourceListResponse(
        String keyword,
        String category,
        int total,
        List<ResourceSummary> resources) {

    public record ResourceSummary(
            Long id,
            String title,
            String category,
            String summary,
            String status,
            String uploaderNickname,
            String fileName,
            Long fileSize,
            Integer downloadCount,
            LocalDateTime publishedAt,
            boolean favoritedByMe) {
    }
}
