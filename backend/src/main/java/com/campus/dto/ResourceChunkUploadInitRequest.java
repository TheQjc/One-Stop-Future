package com.campus.dto;

public record ResourceChunkUploadInitRequest(
        String title,
        String category,
        String summary,
        String description,
        String fileName,
        String contentType,
        Long fileSize,
        Integer chunkSize) {
}
