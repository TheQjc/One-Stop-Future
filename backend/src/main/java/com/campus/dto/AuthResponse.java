package com.campus.dto;

public record AuthResponse(String token, Long userId, String phone, String nickname, String role,
        String status, String verificationStatus) {
}
