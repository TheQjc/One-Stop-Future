package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommunityPostDetailResponse(
        Long id,
        String tag,
        String title,
        String content,
        String status,
        AuthorSummary author,
        int likeCount,
        int commentCount,
        int favoriteCount,
        boolean likedByMe,
        boolean favoritedByMe,
        ExperienceSummary experience,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentItem> comments) {

    public record AuthorSummary(
            Long userId,
            String nickname,
            String role,
            String verificationStatus) {
    }

    public record CommentItem(
            Long id,
            Long authorId,
            String authorNickname,
            String content,
            String status,
            LocalDateTime createdAt,
            boolean mine,
            List<ReplyItem> replies) {
    }

    public record ReplyItem(
            Long id,
            Long authorId,
            String authorNickname,
            Long replyToUserId,
            String replyToUserNickname,
            String content,
            String status,
            LocalDateTime createdAt,
            boolean mine) {
    }

    public record ExperienceSummary(
            boolean enabled,
            String targetLabel,
            String outcomeLabel,
            String timelineSummary,
            String actionSummary) {
    }
}
