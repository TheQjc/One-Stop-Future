package com.campus.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.CommunityCommentStatus;
import com.campus.common.CommunityHotPeriodType;
import com.campus.common.CommunityPostStatus;
import com.campus.common.CommunityTag;
import com.campus.common.FavoriteTargetType;
import com.campus.common.NotificationType;
import com.campus.dto.CommunityHotPostListResponse;
import com.campus.dto.CommunityPostDetailResponse;
import com.campus.dto.CommunityPostListResponse;
import com.campus.dto.CreateCommunityCommentRequest;
import com.campus.dto.CreateCommunityPostRequest;
import com.campus.dto.SearchResponse;
import com.campus.entity.CommunityComment;
import com.campus.entity.CommunityPost;
import com.campus.entity.CommunityPostLike;
import com.campus.entity.User;
import com.campus.entity.UserFavorite;
import com.campus.mapper.CommunityCommentMapper;
import com.campus.mapper.CommunityPostLikeMapper;
import com.campus.mapper.CommunityPostMapper;
import com.campus.mapper.UserFavoriteMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class CommunityService {

    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final int DEFAULT_HOT_LIMIT = 3;
    private static final int MAX_HOT_LIMIT = 10;
    private static final int CONTENT_PREVIEW_LIMIT = 140;
    private static final int EXPERIENCE_TARGET_LABEL_LIMIT = 120;
    private static final int EXPERIENCE_OUTCOME_LABEL_LIMIT = 120;
    private static final int EXPERIENCE_TIMELINE_SUMMARY_LIMIT = 255;
    private static final int EXPERIENCE_ACTION_SUMMARY_LIMIT = 500;

    private final CommunityPostMapper communityPostMapper;
    private final CommunityCommentMapper communityCommentMapper;
    private final CommunityPostLikeMapper communityPostLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserService userService;
    private final NotificationService notificationService;
    private final PlatformCacheService cacheService;

    public CommunityService(CommunityPostMapper communityPostMapper, CommunityCommentMapper communityCommentMapper,
            CommunityPostLikeMapper communityPostLikeMapper, UserFavoriteMapper userFavoriteMapper,
            UserService userService, NotificationService notificationService, PlatformCacheService cacheService) {
        this.communityPostMapper = communityPostMapper;
        this.communityCommentMapper = communityCommentMapper;
        this.communityPostLikeMapper = communityPostLikeMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userService = userService;
        this.notificationService = notificationService;
        this.cacheService = cacheService;
    }

    public CommunityHotPostListResponse listHotPosts(String period, Integer limit) {
        CommunityHotPeriodType normalizedPeriod = normalizeHotPeriod(period);
        int normalizedLimit = normalizeHotLimit(limit);
        String cacheKey = "campus:community:hot:%s:%d".formatted(normalizedPeriod.name(), normalizedLimit);
        return cacheService.getOrLoad(cacheKey, new TypeReference<CommunityHotPostListResponse>() {
        }, () -> listHotPostsFromSource(normalizedPeriod, normalizedLimit));
    }

    private CommunityHotPostListResponse listHotPostsFromSource(CommunityHotPeriodType normalizedPeriod,
            int normalizedLimit) {
        List<CommunityHotPostListResponse.HotPostItem> filteredItems = communityPostMapper.selectList(
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getStatus, CommunityPostStatus.PUBLISHED.name()))
                .stream()
                .filter(post -> matchesHotPeriod(post.getCreatedAt(), normalizedPeriod))
                .map(post -> toHotPostItem(post, normalizedPeriod))
                .sorted(hotRankingComparator())
                .toList();
        return new CommunityHotPostListResponse(
                normalizedPeriod.name(),
                filteredItems.size(),
                filteredItems.stream().limit(normalizedLimit).toList());
    }

    public CommunityPostListResponse listPosts(String tag, String identity) {
        User viewer = findViewer(identity);
        String normalizedTag = normalizeTag(tag, false);

        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, CommunityPostStatus.PUBLISHED.name())
                .orderByDesc(CommunityPost::getCreatedAt)
                .orderByDesc(CommunityPost::getId)
                .last("LIMIT " + DEFAULT_LIST_LIMIT);
        if (normalizedTag != null) {
            wrapper.eq(CommunityPost::getTag, normalizedTag);
        }

        List<CommunityPostListResponse.PostSummary> posts = communityPostMapper.selectList(wrapper).stream()
                .map(post -> toPostSummary(post, viewer))
                .toList();
        return new CommunityPostListResponse(normalizedTag, posts.size(), posts);
    }

    public CommunityPostListResponse listMyPosts(String identity) {
        User viewer = userService.requireByIdentity(identity);
        List<CommunityPostListResponse.PostSummary> posts = communityPostMapper.selectList(
                new LambdaQueryWrapper<CommunityPost>()
                        .eq(CommunityPost::getAuthorId, viewer.getId())
                        .orderByDesc(CommunityPost::getCreatedAt)
                        .orderByDesc(CommunityPost::getId)
                        .last("LIMIT " + DEFAULT_LIST_LIMIT))
                .stream()
                .map(post -> toPostSummary(post, viewer))
                .toList();
        return new CommunityPostListResponse(null, posts.size(), posts);
    }

    public CommunityPostListResponse listMyPostFavorites(String identity, String type) {
        User viewer = userService.requireByIdentity(identity);
        String normalizedType = normalizeFavoriteType(type);
        List<CommunityPostListResponse.PostSummary> posts = userFavoriteMapper.selectList(
                new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, viewer.getId())
                        .eq(UserFavorite::getTargetType, normalizedType)
                        .orderByDesc(UserFavorite::getCreatedAt)
                        .orderByDesc(UserFavorite::getId)
                        .last("LIMIT " + DEFAULT_LIST_LIMIT))
                .stream()
                .map(favorite -> communityPostMapper.selectById(favorite.getTargetId()))
                .filter(post -> post != null && CommunityPostStatus.PUBLISHED.name().equals(post.getStatus()))
                .map(post -> toPostSummary(post, viewer))
                .toList();
        return new CommunityPostListResponse(null, posts.size(), posts);
    }

    public CommunityPostDetailResponse getPostDetail(Long postId, String identity) {
        return toPostDetail(requirePublishedPost(postId), findViewer(identity));
    }

    public List<SearchResponse.SearchResultItem> searchPublishedPosts(String keyword) {
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword == null) {
            return List.of();
        }

        return communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, CommunityPostStatus.PUBLISHED.name())
                .orderByDesc(CommunityPost::getCreatedAt)
                .orderByDesc(CommunityPost::getId))
                .stream()
                .filter(post -> containsKeyword(post.getTitle(), normalizedKeyword)
                        || containsKeyword(post.getContent(), normalizedKeyword))
                .map(post -> new SearchResponse.SearchResultItem(
                        post.getId(),
                        "POST",
                        post.getTitle(),
                        abbreviate(post.getContent(), CONTENT_PREVIEW_LIMIT),
                        authorNicknameOf(post.getAuthorId()),
                        post.getTag(),
                        "/community/" + post.getId(),
                        post.getCreatedAt()))
                .toList();
    }

    public List<CommunityPost> listPublishedDiscoverPosts() {
        return communityPostMapper.selectList(new LambdaQueryWrapper<CommunityPost>()
                .eq(CommunityPost::getStatus, CommunityPostStatus.PUBLISHED.name())
                .orderByDesc(CommunityPost::getCreatedAt)
                .orderByDesc(CommunityPost::getId));
    }

    @Transactional
    public CommunityPostDetailResponse createPost(String identity, CreateCommunityPostRequest request) {
        User author = userService.requireByIdentity(identity);
        CommunityPost post = new CommunityPost();
        LocalDateTime now = LocalDateTime.now();
        boolean experiencePost = Boolean.TRUE.equals(request.experiencePost());
        post.setAuthorId(author.getId());
        post.setTag(normalizeTag(request.tag(), true));
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setStatus(CommunityPostStatus.PUBLISHED.name());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setIsExperiencePost(experiencePost);
        post.setExperienceTargetLabel(experiencePost
                ? normalizeExperienceField(request.experienceTargetLabel(), EXPERIENCE_TARGET_LABEL_LIMIT,
                        "invalid experience target label")
                : null);
        post.setExperienceOutcomeLabel(experiencePost
                ? normalizeExperienceField(request.experienceOutcomeLabel(), EXPERIENCE_OUTCOME_LABEL_LIMIT,
                        "invalid experience outcome label")
                : null);
        post.setExperienceTimelineSummary(experiencePost
                ? normalizeExperienceField(request.experienceTimelineSummary(), EXPERIENCE_TIMELINE_SUMMARY_LIMIT,
                        "invalid experience timeline summary")
                : null);
        post.setExperienceActionSummary(experiencePost
                ? normalizeExperienceField(request.experienceActionSummary(), EXPERIENCE_ACTION_SUMMARY_LIMIT,
                        "invalid experience action summary")
                : null);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        communityPostMapper.insert(post);
        return toPostDetail(post, author);
    }

    @Transactional
    public CommunityPostDetailResponse createComment(String identity, Long postId, CreateCommunityCommentRequest request) {
        User author = userService.requireByIdentity(identity);
        CommunityPost post = requirePublishedPost(postId);
        CommunityComment comment = new CommunityComment();
        LocalDateTime now = LocalDateTime.now();
        comment.setPostId(post.getId());
        comment.setAuthorId(author.getId());
        comment.setParentCommentId(null);
        comment.setReplyToUserId(null);
        comment.setContent(request.content().trim());
        comment.setStatus(CommunityCommentStatus.VISIBLE.name());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        communityCommentMapper.insert(comment);
        recalculatePostStats(post.getId());
        return toPostDetail(requirePublishedPost(post.getId()), author);
    }

    @Transactional
    public CommunityPostDetailResponse createReply(String identity, Long targetCommentId,
            CreateCommunityCommentRequest request) {
        User author = userService.requireByIdentity(identity);
        CommunityComment targetComment = requireVisibleComment(targetCommentId);
        if (targetComment.getParentCommentId() != null) {
            throw new BusinessException(400, "cannot reply to a reply");
        }

        CommunityPost post = requirePublishedPost(targetComment.getPostId());
        CommunityComment reply = new CommunityComment();
        LocalDateTime now = LocalDateTime.now();
        reply.setPostId(post.getId());
        reply.setAuthorId(author.getId());
        reply.setParentCommentId(targetComment.getId());
        reply.setReplyToUserId(targetComment.getAuthorId());
        reply.setContent(request.content().trim());
        reply.setStatus(CommunityCommentStatus.VISIBLE.name());
        reply.setCreatedAt(now);
        reply.setUpdatedAt(now);
        communityCommentMapper.insert(reply);

        if (!author.getId().equals(targetComment.getAuthorId())) {
            notificationService.createNotification(
                    targetComment.getAuthorId(),
                    NotificationType.COMMUNITY_REPLY_RECEIVED.name(),
                    "Your comment received a reply",
                    author.getNickname() + " replied to your comment under \"" + post.getTitle() + "\"",
                    "COMMUNITY_POST",
                    post.getId());
        }

        recalculatePostStats(post.getId());
        return toPostDetail(requirePublishedPost(post.getId()), author);
    }

    @Transactional
    public CommunityPostDetailResponse likePost(String identity, Long postId) {
        User viewer = userService.requireByIdentity(identity);
        CommunityPost post = requirePublishedPost(postId);
        if (!hasLike(post.getId(), viewer.getId())) {
            CommunityPostLike like = new CommunityPostLike();
            like.setPostId(post.getId());
            like.setUserId(viewer.getId());
            like.setCreatedAt(LocalDateTime.now());
            communityPostLikeMapper.insert(like);
        }
        recalculatePostStats(post.getId());
        return toPostDetail(requirePublishedPost(post.getId()), viewer);
    }

    @Transactional
    public CommunityPostDetailResponse unlikePost(String identity, Long postId) {
        User viewer = userService.requireByIdentity(identity);
        CommunityPost post = requirePublishedPost(postId);
        CommunityPostLike existing = communityPostLikeMapper.selectOne(new LambdaQueryWrapper<CommunityPostLike>()
                .eq(CommunityPostLike::getPostId, post.getId())
                .eq(CommunityPostLike::getUserId, viewer.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            communityPostLikeMapper.deleteById(existing.getId());
        }
        recalculatePostStats(post.getId());
        return toPostDetail(requirePublishedPost(post.getId()), viewer);
    }

    @Transactional
    public CommunityPostDetailResponse favoritePost(String identity, Long postId) {
        User viewer = userService.requireByIdentity(identity);
        CommunityPost post = requirePublishedPost(postId);
        if (!hasFavorite(post.getId(), viewer.getId())) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(viewer.getId());
            favorite.setTargetType(FavoriteTargetType.POST.name());
            favorite.setTargetId(post.getId());
            favorite.setCreatedAt(LocalDateTime.now());
            userFavoriteMapper.insert(favorite);
        }
        recalculatePostStats(post.getId());
        return toPostDetail(requirePublishedPost(post.getId()), viewer);
    }

    @Transactional
    public CommunityPostDetailResponse unfavoritePost(String identity, Long postId) {
        User viewer = userService.requireByIdentity(identity);
        CommunityPost post = requirePublishedPost(postId);
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, viewer.getId())
                .eq(UserFavorite::getTargetType, FavoriteTargetType.POST.name())
                .eq(UserFavorite::getTargetId, post.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
        }
        recalculatePostStats(post.getId());
        return toPostDetail(requirePublishedPost(post.getId()), viewer);
    }

    private CommunityPost requirePublishedPost(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null || !CommunityPostStatus.PUBLISHED.name().equals(post.getStatus())) {
            throw new BusinessException(404, "community post not found");
        }
        return post;
    }

    private String normalizeTag(String tag, boolean required) {
        if (tag == null || tag.isBlank()) {
            if (required) {
                throw new BusinessException(400, "invalid community tag");
            }
            return null;
        }
        try {
            return CommunityTag.valueOf(tag.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid community tag");
        }
    }

    private CommunityHotPeriodType normalizeHotPeriod(String period) {
        if (period == null || period.isBlank()) {
            return CommunityHotPeriodType.WEEK;
        }
        try {
            return CommunityHotPeriodType.valueOf(period.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid community hot period");
        }
    }

    private int normalizeHotLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HOT_LIMIT;
        }
        if (limit < 1 || limit > MAX_HOT_LIMIT) {
            throw new BusinessException(400, "invalid community hot limit");
        }
        return limit;
    }

    private String normalizeFavoriteType(String type) {
        if (type == null || type.isBlank()) {
            return FavoriteTargetType.POST.name();
        }
        try {
            return FavoriteTargetType.valueOf(type.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid favorite type");
        }
    }

    private User findViewer(String identity) {
        if (identity == null || identity.isBlank() || "anonymousUser".equals(identity)) {
            return null;
        }
        return userService.requireByIdentity(identity);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void recalculatePostStats(Long postId) {
        CommunityPost post = communityPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "community post not found");
        }
        int commentCount = communityCommentMapper.selectCount(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getStatus, CommunityCommentStatus.VISIBLE.name())).intValue();
        int likeCount = communityPostLikeMapper.selectCount(new LambdaQueryWrapper<CommunityPostLike>()
                .eq(CommunityPostLike::getPostId, postId)).intValue();
        int favoriteCount = userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getTargetType, FavoriteTargetType.POST.name())
                .eq(UserFavorite::getTargetId, postId)).intValue();
        post.setCommentCount(commentCount);
        post.setLikeCount(likeCount);
        post.setFavoriteCount(favoriteCount);
        post.setUpdatedAt(LocalDateTime.now());
        communityPostMapper.updateById(post);
    }

    private CommunityComment requireVisibleComment(Long commentId) {
        CommunityComment comment = communityCommentMapper.selectById(commentId);
        if (comment == null || !CommunityCommentStatus.VISIBLE.name().equals(comment.getStatus())) {
            throw new BusinessException(404, "community comment not found");
        }
        return comment;
    }

    private boolean matchesHotPeriod(LocalDateTime createdAt, CommunityHotPeriodType periodType) {
        if (createdAt == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return switch (periodType) {
            case DAY -> !createdAt.isBefore(now.minusHours(24));
            case WEEK -> !createdAt.isBefore(now.minusDays(7));
            case ALL -> true;
        };
    }

    private CommunityHotPostListResponse.HotPostItem toHotPostItem(CommunityPost post, CommunityHotPeriodType periodType) {
        User author = userService.findByUserId(post.getAuthorId());
        return new CommunityHotPostListResponse.HotPostItem(
                post.getId(),
                post.getTag(),
                post.getTitle(),
                abbreviate(post.getContent(), CONTENT_PREVIEW_LIMIT),
                post.getAuthorId(),
                author == null ? "Unknown" : author.getNickname(),
                safeCount(post.getLikeCount()),
                safeCount(post.getCommentCount()),
                safeCount(post.getFavoriteCount()),
                post.getCreatedAt(),
                communityHotScore(post, author),
                hotLabelFor(periodType));
    }

    private double communityHotScore(CommunityPost post, User author) {
        double rawHeat = safeCount(post.getLikeCount()) * 3.0
                + safeCount(post.getCommentCount()) * 4.0
                + safeCount(post.getFavoriteCount()) * 5.0
                + verifiedAuthorBonus(author);
        return rawHeat + freshnessBonus(post.getCreatedAt());
    }

    private double verifiedAuthorBonus(User author) {
        return author != null && "VERIFIED".equals(author.getVerificationStatus()) ? 2.0 : 0.0;
    }

    private double freshnessBonus(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0;
        }
        long ageDays = Math.max(0, ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()));
        return Math.max(0, 14 - ageDays);
    }

    private String hotLabelFor(CommunityHotPeriodType periodType) {
        return switch (periodType) {
            case DAY -> "Today spotlight";
            case WEEK -> "Weekly discussion";
            case ALL -> "Sustained discussion";
        };
    }

    private Comparator<CommunityHotPostListResponse.HotPostItem> hotRankingComparator() {
        return Comparator.comparingDouble(CommunityHotPostListResponse.HotPostItem::hotScore)
                .reversed()
                .thenComparing(CommunityHotPostListResponse.HotPostItem::createdAt, Comparator.reverseOrder())
                .thenComparing(CommunityHotPostListResponse.HotPostItem::id, Comparator.reverseOrder());
    }

    private CommunityPostListResponse.PostSummary toPostSummary(CommunityPost post, User viewer) {
        return new CommunityPostListResponse.PostSummary(
                post.getId(),
                post.getTag(),
                post.getTitle(),
                abbreviate(post.getContent(), CONTENT_PREVIEW_LIMIT),
                post.getStatus(),
                post.getAuthorId(),
                authorNicknameOf(post.getAuthorId()),
                safeCount(post.getLikeCount()),
                safeCount(post.getCommentCount()),
                safeCount(post.getFavoriteCount()),
                viewer != null && hasLike(post.getId(), viewer.getId()),
                viewer != null && hasFavorite(post.getId(), viewer.getId()),
                toListExperienceSummary(post),
                post.getCreatedAt());
    }

    private CommunityPostDetailResponse toPostDetail(CommunityPost post, User viewer) {
        User author = userService.findByUserId(post.getAuthorId());
        return new CommunityPostDetailResponse(
                post.getId(),
                post.getTag(),
                post.getTitle(),
                post.getContent(),
                post.getStatus(),
                new CommunityPostDetailResponse.AuthorSummary(
                        post.getAuthorId(),
                        author == null ? "Unknown" : author.getNickname(),
                        author == null ? null : author.getRole(),
                        author == null ? null : author.getVerificationStatus()),
                safeCount(post.getLikeCount()),
                safeCount(post.getCommentCount()),
                safeCount(post.getFavoriteCount()),
                viewer != null && hasLike(post.getId(), viewer.getId()),
                viewer != null && hasFavorite(post.getId(), viewer.getId()),
                toDetailExperienceSummary(post),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                loadVisibleComments(post.getId(), viewer == null ? null : viewer.getId()));
    }

    private CommunityPostListResponse.ExperienceSummary toListExperienceSummary(CommunityPost post) {
        boolean enabled = Boolean.TRUE.equals(post.getIsExperiencePost());
        return new CommunityPostListResponse.ExperienceSummary(
                enabled,
                enabled ? post.getExperienceTargetLabel() : null,
                enabled ? post.getExperienceOutcomeLabel() : null,
                enabled ? post.getExperienceTimelineSummary() : null,
                enabled ? post.getExperienceActionSummary() : null);
    }

    private CommunityPostDetailResponse.ExperienceSummary toDetailExperienceSummary(CommunityPost post) {
        boolean enabled = Boolean.TRUE.equals(post.getIsExperiencePost());
        return new CommunityPostDetailResponse.ExperienceSummary(
                enabled,
                enabled ? post.getExperienceTargetLabel() : null,
                enabled ? post.getExperienceOutcomeLabel() : null,
                enabled ? post.getExperienceTimelineSummary() : null,
                enabled ? post.getExperienceActionSummary() : null);
    }

    private String authorNicknameOf(Long authorId) {
        User author = userService.findByUserId(authorId);
        return author == null ? "Unknown" : author.getNickname();
    }

    private List<CommunityPostDetailResponse.CommentItem> loadVisibleComments(Long postId, Long viewerId) {
        List<CommunityComment> visibleComments = communityCommentMapper.selectList(new LambdaQueryWrapper<CommunityComment>()
                .eq(CommunityComment::getPostId, postId)
                .eq(CommunityComment::getStatus, CommunityCommentStatus.VISIBLE.name())
                .orderByAsc(CommunityComment::getCreatedAt)
                .orderByAsc(CommunityComment::getId));
        Map<Long, String> nicknameCache = new LinkedHashMap<>();
        Map<Long, LoadedCommentThread> topLevelThreads = new LinkedHashMap<>();

        for (CommunityComment comment : visibleComments) {
            if (comment.getParentCommentId() != null) {
                continue;
            }
            topLevelThreads.put(comment.getId(), new LoadedCommentThread(
                    comment,
                    nicknameOf(comment.getAuthorId(), nicknameCache),
                    viewerId != null && viewerId.equals(comment.getAuthorId()),
                    new ArrayList<>()));
        }

        for (CommunityComment comment : visibleComments) {
            if (comment.getParentCommentId() == null) {
                continue;
            }
            LoadedCommentThread parentThread = topLevelThreads.get(comment.getParentCommentId());
            if (parentThread == null) {
                continue;
            }
            parentThread.replies().add(new CommunityPostDetailResponse.ReplyItem(
                    comment.getId(),
                    comment.getAuthorId(),
                    nicknameOf(comment.getAuthorId(), nicknameCache),
                    comment.getReplyToUserId(),
                    nicknameOf(comment.getReplyToUserId(), nicknameCache),
                    comment.getContent(),
                    comment.getStatus(),
                    comment.getCreatedAt(),
                    viewerId != null && viewerId.equals(comment.getAuthorId())));
        }

        return topLevelThreads.values().stream()
                .map(thread -> new CommunityPostDetailResponse.CommentItem(
                        thread.comment().getId(),
                        thread.comment().getAuthorId(),
                        thread.authorNickname(),
                        thread.comment().getContent(),
                        thread.comment().getStatus(),
                        thread.comment().getCreatedAt(),
                        thread.mine(),
                        List.copyOf(thread.replies())))
                .toList();
    }

    private String nicknameOf(Long userId, Map<Long, String> nicknameCache) {
        if (userId == null) {
            return "Unknown";
        }
        return nicknameCache.computeIfAbsent(userId, this::authorNicknameOf);
    }

    private boolean hasLike(Long postId, Long userId) {
        return communityPostLikeMapper.selectCount(new LambdaQueryWrapper<CommunityPostLike>()
                .eq(CommunityPostLike::getPostId, postId)
                .eq(CommunityPostLike::getUserId, userId)) > 0;
    }

    private boolean hasFavorite(Long postId, Long userId) {
        return userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, FavoriteTargetType.POST.name())
                .eq(UserFavorite::getTargetId, postId)) > 0;
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private String abbreviate(String content, int limit) {
        if (content == null || content.length() <= limit) {
            return content;
        }
        return content.substring(0, limit) + "...";
    }

    private String normalizeExperienceField(String value, int maxLength, String errorMessage) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(400, errorMessage);
        }
        return normalized;
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private record LoadedCommentThread(
            CommunityComment comment,
            String authorNickname,
            boolean mine,
            List<CommunityPostDetailResponse.ReplyItem> replies) {
    }
}
