package com.campus.dto;

import java.util.List;

public record ResourceChunkUploadStatusResponse(
        String uploadId,
        String fileName,
        long fileSize,
        int chunkSize,
        int totalChunks,
        List<Integer> uploadedChunks,
        long uploadedBytes,
        boolean complete) {
}
