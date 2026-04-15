package com.campus.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.NotificationListResponse;
import com.campus.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Result<NotificationListResponse> list(Authentication authentication) {
        return Result.success(notificationService.listByIdentity(authentication.getName()));
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markRead(authentication.getName(), id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Map<String, Integer>> markAllRead(Authentication authentication) {
        return Result.success(Map.of("updatedCount", notificationService.markAllRead(authentication.getName())));
    }
}
