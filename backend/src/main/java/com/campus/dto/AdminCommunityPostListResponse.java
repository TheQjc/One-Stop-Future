package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCommunityPostListResponse(
        int total,
        List<PostItem> posts) {

    public record PostItem(
            Long id,
            String tag,
            String title,
            String status,
            Long authorId,
            String authorNickname,
            int likeCount,
            int commentCount,
            int favoriteCount,
            LocalDateTime createdAt) {
    }
}
