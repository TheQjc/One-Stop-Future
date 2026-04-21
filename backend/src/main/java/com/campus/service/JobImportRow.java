package com.campus.service;

public record JobImportRow(
        int rowNumber,
        String title,
        String companyName,
        String city,
        String jobType,
        String educationRequirement,
        String sourcePlatform,
        String sourceUrl,
        String summary,
        String content,
        String deadlineAt) {
}
