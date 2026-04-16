package com.campus.dto;

import java.time.LocalDateTime;

public record ResourceDetailResponse(
        Long id,
        String title,
        String category,
        String summary,
        String description,
        String status,
        Long uploaderId,
        String uploaderNickname,
        String fileName,
        String fileExt,
        String contentType,
        Long fileSize,
        Integer downloadCount,
        Integer favoriteCount,
        LocalDateTime publishedAt,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String rejectReason,
        boolean favoritedByMe,
        boolean editableByMe,
        boolean previewAvailable) {
}
