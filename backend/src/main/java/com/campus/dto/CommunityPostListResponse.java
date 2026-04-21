package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostListResponse(
        String selectedTag,
        int total,
        List<PostSummary> posts) {

    public record PostSummary(
            Long id,
            String tag,
            String title,
            String contentPreview,
            String status,
            Long authorId,
            String authorNickname,
            int likeCount,
            int commentCount,
            int favoriteCount,
            boolean likedByMe,
            boolean favoritedByMe,
            ExperienceSummary experience,
            LocalDateTime createdAt) {
    }

    public record ExperienceSummary(
            boolean enabled,
            String targetLabel,
            String outcomeLabel,
            String timelineSummary,
            String actionSummary) {
    }
}
