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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理-社区", description = "社区帖子与评论管理")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/community")
public class AdminCommunityController {

    private final AdminCommunityService adminCommunityService;

    public AdminCommunityController(AdminCommunityService adminCommunityService) {
        this.adminCommunityService = adminCommunityService;
    }

    @Operation(summary = "获取帖子列表")
    @GetMapping("/posts")
    public Result<AdminCommunityPostListResponse> list() {
        return Result.success(adminCommunityService.listPosts());
    }

    @Operation(summary = "隐藏帖子")
    @ApiResponse(responseCode = "200", description = "隐藏成功")
    @PostMapping("/posts/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        adminCommunityService.hidePost(id);
        return Result.success();
    }

    @Operation(summary = "删除帖子")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PostMapping("/posts/{id}/delete")
    public Result<Void> delete(@PathVariable Long id) {
        adminCommunityService.deletePost(id);
        return Result.success();
    }
}
