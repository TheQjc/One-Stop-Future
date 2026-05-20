package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResourceVersionListResponse(
        int total,
        List<ResourceVersionItem> versions) {

    public record ResourceVersionItem(
            Long id,
            Long resourceId,
            Integer versionNo,
            String changeType,
            String title,
            String category,
            String summary,
            String description,
            String status,
            String fileName,
            String fileExt,
            String contentType,
            Long fileSize,
            Long operatorId,
            LocalDateTime createdAt) {
    }
}
