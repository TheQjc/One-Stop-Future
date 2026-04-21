package com.campus.dto;

public record AdminJobImportValidationError(int rowNumber, String column, String message) {
}
