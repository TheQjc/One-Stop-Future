package com.campus.dto;

import java.util.List;

public record ResourceZipPreviewResponse(
        Long resourceId,
        String fileName,
        int entryCount,
        List<Entry> entries) {

    public record Entry(
            String path,
            String name,
            boolean directory,
            Long size) {
    }
}
