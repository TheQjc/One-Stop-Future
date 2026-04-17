package com.campus.dto;

import java.util.List;

public record AdminResourceMigrationResponse(
        boolean dryRun,
        int requestedLimit,
        int matchedCount,
        int processedCount,
        int successCount,
        int skippedCount,
        int failureCount,
        List<Item> items) {

    public record Item(
            Long resourceId,
            String title,
            String status,
            String storageKey,
            String outcome,
            String message) {
    }
}
