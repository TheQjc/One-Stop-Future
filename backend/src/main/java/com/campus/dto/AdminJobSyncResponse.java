package com.campus.dto;

import java.util.List;

public record AdminJobSyncResponse(
        String sourceName,
        int fetchedCount,
        int createdCount,
        int updatedCount,
        int skippedCount,
        int invalidCount,
        String defaultCreatedStatus,
        List<Issue> issues) {

    public record Issue(
            int itemIndex,
            String sourceUrl,
            String type,
            String message) {
    }
}
