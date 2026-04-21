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

@Validated
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/hot")
    public Result<CommunityHotPostListResponse> hot(@RequestParam(required = false) String period,
            @RequestParam(required = false) Integer limit) {
        return Result.success(communityService.listHotPosts(period, limit));
    }

    @GetMapping("/posts")
    public Result<CommunityPostListResponse> list(@RequestParam(required = false) String tag,
            Authentication authentication) {
        return Result.success(communityService.listPosts(tag, identityOf(authentication)));
    }

    @GetMapping("/posts/mine")
    public Result<CommunityPostListResponse> mine(Authentication authentication) {
        return Result.success(communityService.listMyPosts(authentication.getName()));
    }

    @GetMapping("/posts/{id}")
    public Result<CommunityPostDetailResponse> detail(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.getPostDetail(id, identityOf(authentication)));
    }

    @PostMapping("/posts")
    public Result<CommunityPostDetailResponse> create(Authentication authentication,
            @Validated @RequestBody CreateCommunityPostRequest request) {
        return Result.success(communityService.createPost(authentication.getName(), request));
    }

    @PostMapping("/posts/{id}/comments")
    public Result<CommunityPostDetailResponse> comment(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody CreateCommunityCommentRequest request) {
        return Result.success(communityService.createComment(authentication.getName(), id, request));
    }

    @PostMapping("/comments/{id}/replies")
    public Result<CommunityPostDetailResponse> reply(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody CreateCommunityCommentRequest request) {
        return Result.success(communityService.createReply(authentication.getName(), id, request));
    }

    @PostMapping("/posts/{id}/like")
    public Result<CommunityPostDetailResponse> like(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.likePost(authentication.getName(), id));
    }

    @DeleteMapping("/posts/{id}/like")
    public Result<CommunityPostDetailResponse> unlike(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.unlikePost(authentication.getName(), id));
    }

    @PostMapping("/posts/{id}/favorite")
    public Result<CommunityPostDetailResponse> favorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(communityService.favoritePost(authentication.getName(), id));
    }

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
