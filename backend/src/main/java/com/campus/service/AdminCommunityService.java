package com.campus.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.CommunityPostStatus;
import com.campus.dto.AdminCommunityPostListResponse;
import com.campus.entity.CommunityPost;
import com.campus.entity.User;
import com.campus.mapper.CommunityPostMapper;

@Service
public class AdminCommunityService {

    private static final int ADMIN_LIST_LIMIT = 100;

    private final CommunityPostMapper communityPostMapper;
    private final UserService userService;

    public AdminCommunityService(CommunityPostMapper communityPostMapper, UserService userService) {
        this.communityPostMapper = communityPostMapper;
        this.userService = userService;
    }

    public AdminCommunityPostListResponse listPosts() {
        List<AdminCommunityPostListResponse.PostItem> posts = communityPostMapper.selectList(
                new LambdaQueryWrapper<CommunityPost>()
                        .orderByDesc(CommunityPost::getCreatedAt)
                        .orderByDesc(CommunityPost::getId)
                        .last("LIMIT " + ADMIN_LIST_LIMIT))
                .stream()
                .map(this::toPostItem)
                .toList();
        return new AdminCommunityPostListResponse(posts.size(), posts);
    }

    @Transactional
    public void hidePost(Long postId) {
        CommunityPost post = requirePost(postId);
        if (CommunityPostStatus.DELETED.name().equals(post.getStatus())) {
            throw new BusinessException(400, "deleted post cannot be hidden");
        }
        if (CommunityPostStatus.HIDDEN.name().equals(post.getStatus())) {
            return;
        }
        post.setStatus(CommunityPostStatus.HIDDEN.name());
        post.setUpdatedAt(LocalDateTime.now());
        communityPostMapper.updateById(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        CommunityPost post = requirePost(postId);
        if (CommunityPostStatus.DELETED.name().equals(post.getStatus())) {
            return;
        }
        post.setStatus(CommunityPostStatus.DELETED.name());
        post.setUpdatedAt(LocalDateTime.now());
        communityPostMapper.updateById(post);
    }

    private CommunityPost requirePost(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "community post not found");
        }
        return post;
    }

    private AdminCommunityPostListResponse.PostItem toPostItem(CommunityPost post) {
        User author = userService.findByUserId(post.getAuthorId());
        return new AdminCommunityPostListResponse.PostItem(
                post.getId(),
                post.getTag(),
                post.getTitle(),
                post.getStatus(),
                post.getAuthorId(),
                author == null ? "Unknown" : author.getNickname(),
                post.getLikeCount() == null ? 0 : post.getLikeCount(),
                post.getCommentCount() == null ? 0 : post.getCommentCount(),
                post.getFavoriteCount() == null ? 0 : post.getFavoriteCount(),
                post.getCreatedAt());
    }
}
