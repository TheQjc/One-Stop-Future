package com.campus.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserListResponse(
        int total,
        int activeCount,
        int bannedCount,
        int verifiedCount,
        List<UserItem> users) {

    public record UserItem(
            Long id,
            String phone,
            String nickname,
            String realName,
            String role,
            String status,
            String verificationStatus,
            String studentId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
