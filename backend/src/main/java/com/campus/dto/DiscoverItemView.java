package com.campus.dto;

import java.time.LocalDateTime;

public record DiscoverItemView(
        Long id,
        String type,
        String title,
        String summary,
        String primaryMeta,
        String secondaryMeta,
        String path,
        LocalDateTime publishedAt,
        double hotScore,
        String hotLabel) {
}
