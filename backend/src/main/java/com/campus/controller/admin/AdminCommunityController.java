package com.campus.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AdminCommunityPostListResponse;
import com.campus.service.AdminCommunityService;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/community")
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    public AdminCommunityController(AdminCommunityService adminCommunityService) {
        this.adminCommunityService = adminCommunityService;
    }

    @GetMapping("/posts")
    public Result<AdminCommunityPostListResponse> list() {
        return Result.success(adminCommunityService.listPosts());
    }

    @PostMapping("/posts/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        adminCommunityService.hidePost(id);
        return Result.success();
    }

    @PostMapping("/posts/{id}/delete")
    public Result<Void> delete(@PathVariable Long id) {
        adminCommunityService.deletePost(id);
        return Result.success();
    }
}
