package com.campus.dto;

import java.time.LocalDateTime;

public record JobApplicationRecordResponse(
        Long id,
        Long jobId,
        String status,
        Long resumeId,
        String resumeTitleSnapshot,
        String resumeFileNameSnapshot,
        LocalDateTime submittedAt) {
}
