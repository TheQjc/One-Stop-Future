package com.campus.common;

import com.campus.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionContext {

    private final Long userId;
    private final String userRole;
    private final String verificationStatus;

    public boolean isAdmin() {
        return "ADMIN".equals(userRole);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public boolean isVerified() {
        return "VERIFIED".equals(verificationStatus);
    }

    public boolean canViewPublicContent() {
        return true;
    }

    public boolean canViewPrivateContent(Long ownerId) {
        return ownerId != null && ownerId.equals(userId) || isAdmin();
    }

    public boolean canViewAdminContent() {
        return isAdmin();
    }

    public static PermissionContext forAnonymous() {
        return PermissionContext.builder()
                .userId(null)
                .userRole(null)
                .verificationStatus(null)
                .build();
    }

    public static PermissionContext fromUser(User user) {
        if (user == null) {
            return forAnonymous();
        }
        return PermissionContext.builder()
                .userId(user.getId())
                .userRole(user.getRole())
                .verificationStatus(user.getVerificationStatus())
                .build();
    }
}
