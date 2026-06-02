package com.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResourceChunkUploadInitRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String category,
        @NotBlank @Size(max = 500) String summary,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize,
        @Positive Integer chunkSize) {
}
