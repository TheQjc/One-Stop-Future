package com.campus.dto;

public record UserProfile(
        Long id,
        String phone,
        String nickname,
        String role,
        String status,
        String verificationStatus,
        String realName,
        String studentId) {
}
