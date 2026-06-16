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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "通知", description = "通知中心与已读管理")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "获取通知列表")
    @GetMapping
    public Result<NotificationListResponse> list(Authentication authentication) {
        return Result.success(notificationService.listByIdentity(authentication.getName()));
    }

    @Operation(summary = "标记通知已读")
    @ApiResponse(responseCode = "200", description = "标记成功")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markRead(authentication.getName(), id);
        return Result.success();
    }

    @Operation(summary = "标记全部已读")
    @ApiResponse(responseCode = "200", description = "标记成功")
    @PostMapping("/read-all")
    public Result<Map<String, Integer>> markAllRead(Authentication authentication) {
        return Result.success(Map.of("updatedCount", notificationService.markAllRead(authentication.getName())));
    }
}
