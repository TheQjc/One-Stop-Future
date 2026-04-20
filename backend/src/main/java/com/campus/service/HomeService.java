package com.campus.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final int HOME_DISCOVER_PREVIEW_LIMIT = 4;

    private static final Logger log = LoggerFactory.getLogger(HomeService.class);

    private final UserService userService;
    private final NotificationService notificationService;
    private final VerificationApplicationMapper verificationApplicationMapper;
    private final DiscoverService discoverService;

    public HomeService(UserService userService, NotificationService notificationService,
            VerificationApplicationMapper verificationApplicationMapper, DiscoverService discoverService) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.verificationApplicationMapper = verificationApplicationMapper;
        this.discoverService = discoverService;
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
                notificationService.listLatestSnippets(user.getId(), HOME_LATEST_NOTIFICATION_LIMIT),
                loadDiscoverPreview());
    }

    private HomeSummaryResponse guestSummary() {
        List<HomeSummaryResponse.HomeEntryCard> entries = List.of(
                new HomeSummaryResponse.HomeEntryCard("community", "Community", "/community", true, null),
                new HomeSummaryResponse.HomeEntryCard("jobs", "Jobs", "/jobs", true, null),
                new HomeSummaryResponse.HomeEntryCard("resources", "Resources", "/resources", true, null),
                new HomeSummaryResponse.HomeEntryCard("assessment", "Assessment", "/assessment", false, "LOGIN_REQUIRED"),
                new HomeSummaryResponse.HomeEntryCard("analytics", "Analytics", "/analytics", true, null));
        return new HomeSummaryResponse(
                "GUEST",
                null,
                "Guest",
                null,
                0,
                List.of("Sign in to unlock profile, verification, and notifications."),
                entries,
                List.of(),
                loadDiscoverPreview());
    }

    private HomeSummaryResponse.DiscoverPreview loadDiscoverPreview() {
        try {
            return new HomeSummaryResponse.DiscoverPreview(
                    "WEEK",
                    discoverService.previewForHome(HOME_DISCOVER_PREVIEW_LIMIT));
        } catch (RuntimeException exception) {
            log.warn("Failed to load home discover preview", exception);
            return new HomeSummaryResponse.DiscoverPreview("WEEK", List.of());
        }
    }

    private String viewerTypeFor(User user) {
        if ("ADMIN".equals(user.getRole())) {
            return "ADMIN";
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
        if ("ADMIN".equals(user.getRole())) {
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
        entries.add(new HomeSummaryResponse.HomeEntryCard("jobs", "Jobs", "/jobs", true, null));
        entries.add(new HomeSummaryResponse.HomeEntryCard("resources", "Resources", "/resources", true, null));
        entries.add(new HomeSummaryResponse.HomeEntryCard("assessment", "Assessment", "/assessment", true, null));
        entries.add(new HomeSummaryResponse.HomeEntryCard("analytics", "Analytics", "/analytics", true, null));
        if ("ADMIN".equals(user.getRole())) {
            entries.add(new HomeSummaryResponse.HomeEntryCard("admin-dashboard", "Admin Dashboard",
                    "/admin/dashboard", true, null));
            entries.add(new HomeSummaryResponse.HomeEntryCard("admin-users", "Admin User Desk",
                    "/admin/users", true, null));
            entries.add(new HomeSummaryResponse.HomeEntryCard("admin-verifications", "Admin Verification Review",
                    "/admin/verifications", true, null));
        }
        return entries;
    }
}
