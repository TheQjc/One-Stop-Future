package com.campus.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AdminUserListResponse;
import com.campus.service.AdminUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理-用户", description = "用户管理与状态变更")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "获取用户列表")
    @GetMapping
    public Result<AdminUserListResponse> list(Authentication authentication) {
        return Result.success(adminUserService.listUsers(authentication.getName()));
    }

    @Operation(summary = "封禁用户")
    @ApiResponse(responseCode = "200", description = "封禁成功")
    @PostMapping("/{id}/ban")
    public Result<AdminUserListResponse.UserItem> ban(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminUserService.banUser(authentication.getName(), id));
    }

    @Operation(summary = "解封用户")
    @ApiResponse(responseCode = "200", description = "解封成功")
    @PostMapping("/{id}/unban")
    public Result<AdminUserListResponse.UserItem> unban(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminUserService.unbanUser(authentication.getName(), id));
    }
}
