package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.CommunityHotPostListResponse;
import com.campus.dto.CommunityPostDetailResponse;
import com.campus.dto.CommunityPostListResponse;
import com.campus.dto.CreateCommunityCommentRequest;
import com.campus.dto.CreateCommunityPostRequest;
import com.campus.service.CommunityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "社区", description = "帖子浏览、发布、评论、点赞、收藏")
@Validated
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @Operation(summary = "热门帖子", description = "按时间周期（日/周/全部）获取热门帖子排行")
    @GetMapping("/hot")
    public Result<CommunityHotPostListResponse> hot(@RequestParam(required = false) String period,
            @RequestParam(required = false) Integer limit) {
        return Result.success(communityService.listHotPosts(period, limit));
    }

    @Operation(summary = "帖子列表", description = "按标签筛选已发布的帖子列表")
    @GetMapping("/posts")
    public Result<CommunityPostListResponse> list(@RequestParam(required = false) String tag,
            Authentication authentication) {
        return Result.success(communityService.listPosts(tag, identityOf(authentication)));
    }

    @Operation(summary = "我的帖子", description = "获取当前用户发布的帖子列表")
    @GetMapping("/posts/mine")
    public Result<CommunityPostListResponse> mine(Authentication authentication) {
        return Result.success(communityService.listMyPosts(authentication.getName()));
    }

    @Operation(summary = "帖子详情", description = "获取指定帖子的完整内容、评论和点赞信息")
    @GetMapping("/posts/{id}")
    public Result<CommunityPostDetailResponse> detail(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.getPostDetail(id, identityOf(authentication)));
    }

    @Operation(summary = "发布帖子", description = "创建一篇新的社区帖子（支持经验帖）")
    @ApiResponse(responseCode = "200", description = "发布成功")
    @PostMapping("/posts")
    public Result<CommunityPostDetailResponse> create(Authentication authentication,
            @Validated @RequestBody CreateCommunityPostRequest request) {
        return Result.success(communityService.createPost(authentication.getName(), request));
    }

    @Operation(summary = "发表评论", description = "对指定帖子发表顶层评论")
    @ApiResponse(responseCode = "200", description = "评论成功")
    @PostMapping("/posts/{id}/comments")
    public Result<CommunityPostDetailResponse> comment(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody CreateCommunityCommentRequest request) {
        return Result.success(communityService.createComment(authentication.getName(), id, request));
    }

    @Operation(summary = "回复评论", description = "对指定评论发表回复")
    @ApiResponse(responseCode = "200", description = "回复成功")
    @PostMapping("/comments/{id}/replies")
    public Result<CommunityPostDetailResponse> reply(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody CreateCommunityCommentRequest request) {
        return Result.success(communityService.createReply(authentication.getName(), id, request));
    }

    @Operation(summary = "点赞帖子")
    @ApiResponse(responseCode = "200", description = "点赞成功")
    @PostMapping("/posts/{id}/like")
    public Result<CommunityPostDetailResponse> like(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.likePost(authentication.getName(), id));
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/posts/{id}/like")
    public Result<CommunityPostDetailResponse> unlike(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.unlikePost(authentication.getName(), id));
    }

    @Operation(summary = "收藏帖子")
    @ApiResponse(responseCode = "200", description = "收藏成功")
    @PostMapping("/posts/{id}/favorite")
    public Result<CommunityPostDetailResponse> favorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.favoritePost(authentication.getName(), id));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/posts/{id}/favorite")
    public Result<CommunityPostDetailResponse> unfavorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.unfavoritePost(authentication.getName(), id));
    }

    private String identityOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }
}
