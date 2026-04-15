package com.campus.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dto.HomeSummaryResponse;
import com.campus.entity.User;
import com.campus.entity.VerificationApplication;
import com.campus.mapper.VerificationApplicationMapper;

@Service
public class HomeService {

    private static final int HOME_LATEST_NOTIFICATION_LIMIT = 5;

    private final UserService userService;
    private final NotificationService notificationService;
    private final VerificationApplicationMapper verificationApplicationMapper;

    public HomeService(UserService userService, NotificationService notificationService,
            VerificationApplicationMapper verificationApplicationMapper) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.verificationApplicationMapper = verificationApplicationMapper;
    }

    public HomeSummaryResponse getSummary(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return guestSummary();
        }
        User user = userService.requireByIdentity(authentication.getName());
        int unreadCount = notificationService.countUnreadByUserId(user.getId());
        List<String> todos = buildTodos(user, unreadCount);
        List<HomeSummaryResponse.HomeEntryCard> entries = buildEntries(user);
        return new HomeSummaryResponse(
                viewerTypeFor(user),
                new HomeSummaryResponse.IdentitySnapshot(
                        user.getId(),
                        user.getPhone(),
                        user.getNickname(),
                        user.getRole(),
                        user.getVerificationStatus()),
                roleLabelFor(user.getRole()),
                user.getVerificationStatus(),
                unreadCount,
                todos,
                entries,
                notificationService.listLatestSnippets(user.getId(), HOME_LATEST_NOTIFICATION_LIMIT));
    }

    private HomeSummaryResponse guestSummary() {
        List<HomeSummaryResponse.HomeEntryCard> entries = List.of(
                new HomeSummaryResponse.HomeEntryCard("community", "Community", "/community", true, null),
                new HomeSummaryResponse.HomeEntryCard("jobs", "Jobs", "/jobs", true, "COMING_SOON"),
                new HomeSummaryResponse.HomeEntryCard("resources", "Resources", "/resources", true, "COMING_SOON"),
                new HomeSummaryResponse.HomeEntryCard("assessment", "Assessment", "/assessment", false, "LOGIN_REQUIRED"));
        return new HomeSummaryResponse(
                "GUEST",
                null,
                "Guest",
                null,
                0,
                List.of("Sign in to unlock profile, verification, and notifications."),
                entries,
                List.of());
    }

    private String viewerTypeFor(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return "ADMIN";
        }
        if ("TEACHER".equals(user.getRole())) {
            return "TEACHER";
        }
        if ("VERIFIED".equals(user.getVerificationStatus())) {
            return "VERIFIED_USER";
        }
        return "USER";
    }

    private String roleLabelFor(String role) {
        if ("ADMIN".equals(role)) {
            return "Administrator";
        }
        if ("TEACHER".equals(role)) {
            return "Teacher";
        }
        return "User";
    }

    private List<String> buildTodos(User user, int unreadCount) {
        List<String> todos = new ArrayList<>();
        if ("USER".equals(user.getRole()) && "UNVERIFIED".equals(user.getVerificationStatus())) {
            todos.add("Complete student verification.");
        }
        if ("PENDING".equals(user.getVerificationStatus())) {
            todos.add("Student verification is under review.");
        }
        if (unreadCount > 0) {
            todos.add("You have %d unread notifications.".formatted(unreadCount));
        }
        if ("ADMIN".equals(user.getRole()) || "TEACHER".equals(user.getRole())) {
            int pendingCount = verificationApplicationMapper
                    .selectCount(new LambdaQueryWrapper<VerificationApplication>()
                            .eq(VerificationApplication::getStatus, "PENDING"))
                    .intValue();
            if (pendingCount > 0) {
                todos.add("Review %d pending verification applications.".formatted(pendingCount));
            }
        }
        if (todos.isEmpty()) {
            todos.add("No pending tasks.");
        }
        return todos;
    }

    private List<HomeSummaryResponse.HomeEntryCard> buildEntries(User user) {
        List<HomeSummaryResponse.HomeEntryCard> entries = new ArrayList<>();
        entries.add(new HomeSummaryResponse.HomeEntryCard("community", "Community", "/community", true, null));
        entries.add(new HomeSummaryResponse.HomeEntryCard("jobs", "Jobs", "/jobs", true, "COMING_SOON"));
        entries.add(new HomeSummaryResponse.HomeEntryCard("resources", "Resources", "/resources", true, "COMING_SOON"));
        entries.add(new HomeSummaryResponse.HomeEntryCard("assessment", "Assessment", "/assessment", true, "COMING_SOON"));
        entries.add(new HomeSummaryResponse.HomeEntryCard("analytics", "Analytics", "/analytics", true, "COMING_SOON"));
        if ("ADMIN".equals(user.getRole()) || "TEACHER".equals(user.getRole())) {
            entries.add(new HomeSummaryResponse.HomeEntryCard("admin-verifications", "Admin Verification Review",
                    "/admin/verifications", true, null));
        }
        return entries;
    }
}
