package com.campus.dto;

public record AdminJobImportResponse(
        String fileName,
        int totalRows,
        int importedCount,
        String defaultStatus) {
}
