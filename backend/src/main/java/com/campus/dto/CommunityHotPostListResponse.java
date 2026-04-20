package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityHotPostListResponse(
        String period,
        int total,
        List<HotPostItem> items) {

    public record HotPostItem(
            Long id,
            String tag,
            String title,
            String contentPreview,
            Long authorId,
            String authorNickname,
            int likeCount,
            int commentCount,
            int favoriteCount,
            LocalDateTime createdAt,
            double hotScore,
            String hotLabel) {
    }
}
