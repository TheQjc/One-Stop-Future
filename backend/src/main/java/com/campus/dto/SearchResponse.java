package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SearchResponse(
        String query,
        String type,
        String sort,
        SearchTotals totals,
        List<SearchResultItem> results) {

    public record SearchTotals(int all, int post, int job, int resource, int resume, int notification, int application) {
    }

    public record SearchResultItem(
            Long id,
            String type,
            String title,
            String summary,
            String metaPrimary,
            String metaSecondary,
            String path,
            LocalDateTime publishedAt,
            String highlightedTitle,
            String highlightedSummary,
            String highlightedContent) {

        public SearchResultItem(Long id, String type, String title, String summary,
                String metaPrimary, String metaSecondary, String path, LocalDateTime publishedAt) {
            this(id, type, title, summary, metaPrimary, metaSecondary, path, publishedAt, null, null, null);
        }
    }
}
