package com.campus.dto;

import java.time.LocalDateTime;

public record ResumeRecordResponse(
        Long id,
        String title,
        String fileName,
        String fileExt,
        String contentType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
