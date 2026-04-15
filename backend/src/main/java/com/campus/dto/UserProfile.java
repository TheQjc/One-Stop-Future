package com.campus.dto;

import com.campus.common.VerificationStatus;

public record UserProfile(
        Long id,
        String phone,
        String nickname,
        String role,
        String status,
        String verificationStatus,
        String realName,
        String studentId) {

    public UserProfile(Long id, String username, String realName, String role, String email) {
        this(id, username, username, role, "ACTIVE", VerificationStatus.UNVERIFIED.name(), realName, null);
    }
}
