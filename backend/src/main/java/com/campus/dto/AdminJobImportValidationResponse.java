package com.campus.dto;

import java.util.List;

public record AdminJobImportValidationResponse(
        String fileName,
        int totalRows,
        int importedCount,
        List<AdminJobImportValidationError> errors) {
}
