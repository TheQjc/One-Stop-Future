package com.campus.dto;

import java.time.LocalDateTime;

import com.campus.common.ResourcePreviewKind;

public record ResumeRecordResponse(
        Long id,
        String title,
        String fileName,
        String fileExt,
        String contentType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean previewAvailable,
        ResourcePreviewKind previewKind) {
}
