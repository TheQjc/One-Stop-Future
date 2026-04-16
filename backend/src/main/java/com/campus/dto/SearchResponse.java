package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SearchResponse(
        String query,
        String type,
        String sort,
        SearchTotals totals,
        List<SearchResultItem> results) {

    public record SearchTotals(int all, int post, int job, int resource) {
    }

    public record SearchResultItem(
            Long id,
            String type,
            String title,
            String summary,
            String metaPrimary,
            String metaSecondary,
            String path,
            LocalDateTime publishedAt) {
    }
}
