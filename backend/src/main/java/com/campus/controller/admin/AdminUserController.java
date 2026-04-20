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

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Result<AdminUserListResponse> list(Authentication authentication) {
        return Result.success(adminUserService.listUsers(authentication.getName()));
    }

    @PostMapping("/{id}/ban")
    public Result<AdminUserListResponse.UserItem> ban(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminUserService.banUser(authentication.getName(), id));
    }

    @PostMapping("/{id}/unban")
    public Result<AdminUserListResponse.UserItem> unban(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminUserService.unbanUser(authentication.getName(), id));
    }
}
