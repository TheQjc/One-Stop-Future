package com.campus.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.DiscoverContentType;
import com.campus.common.DiscoverPeriodType;
import com.campus.dto.DiscoverItemView;
import com.campus.dto.DiscoverResponse;
import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;
import com.campus.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class DiscoverService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;
    private static final double EXPERIENCE_POST_BONUS = 4.0;

    private final CommunityService communityService;
    private final JobService jobService;
    private final ResourceService resourceService;
    private final UserService userService;
    private final PlatformCacheService cacheService;

    public DiscoverService(CommunityService communityService, JobService jobService, ResourceService resourceService,
            UserService userService, PlatformCacheService cacheService) {
        this.communityService = communityService;
        this.jobService = jobService;
        this.resourceService = resourceService;
        this.userService = userService;
        this.cacheService = cacheService;
    }

    public DiscoverResponse discover(String tab, String period, Integer limit) {
        DiscoverContentType normalizedTab = normalizeTab(tab);
        DiscoverPeriodType normalizedPeriod = normalizePeriod(period);
        int normalizedLimit = normalizeLimit(limit);
        String cacheKey = "campus:discover:%s:%s:%d".formatted(
                normalizedTab.name(), normalizedPeriod.name(), normalizedLimit);
        return cacheService.getOrLoad(cacheKey, new TypeReference<DiscoverResponse>() {
        }, () -> discoverFromSource(normalizedTab, normalizedPeriod, normalizedLimit));
    }

    private DiscoverResponse discoverFromSource(DiscoverContentType normalizedTab, DiscoverPeriodType normalizedPeriod,
            int normalizedLimit) {
        List<DiscoverItemView> postItems = communityService.listPublishedDiscoverPosts().stream()
                .filter(post -> matchesPeriod(post.getCreatedAt(), normalizedPeriod))
                .map(post -> toPostItem(post, normalizedPeriod))
                .toList();
        List<JobPosting> discoverJobs = jobService.listPublishedDiscoverJobs().stream()
                .filter(job -> matchesPeriod(job.getPublishedAt(), normalizedPeriod))
                .toList();
        Map<Long, Integer> jobFavoriteCounts = jobService.favoriteCountsByJobIds(
                discoverJobs.stream().map(JobPosting::getId).toList());
        List<DiscoverItemView> jobItems = discoverJobs.stream()
                .map(job -> toJobItem(job, normalizedPeriod, jobFavoriteCounts.getOrDefault(job.getId(), 0)))
                .toList();
        List<DiscoverItemView> resourceItems = resourceService.listPublishedDiscoverResources().stream()
                .filter(resource -> matchesPeriod(resource.getPublishedAt(), normalizedPeriod))
                .map(resource -> toResourceItem(resource, normalizedPeriod))
                .toList();

        List<DiscoverItemView> filteredItems = switch (normalizedTab) {
            case ALL -> mergeResults(postItems, jobItems, resourceItems);
            case POST -> postItems;
            case JOB -> jobItems;
            case RESOURCE -> resourceItems;
        };
        List<DiscoverItemView> items = filteredItems.stream()
                .sorted(discoverComparator())
                .limit(normalizedLimit)
                .toList();

        return new DiscoverResponse(
                normalizedTab.name(),
                normalizedPeriod.name(),
                filteredItems.size(),
                items);
    }

    public List<DiscoverItemView> previewForHome(int limit) {
        return discover(DiscoverContentType.ALL.name(), DiscoverPeriodType.WEEK.name(), limit).items();
    }

    private DiscoverContentType normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return DiscoverContentType.ALL;
        }
        try {
            return DiscoverContentType.valueOf(tab.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid discover tab");
        }
    }

    private DiscoverPeriodType normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return DiscoverPeriodType.WEEK;
        }
        try {
            return DiscoverPeriodType.valueOf(period.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid discover period");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(400, "invalid discover limit");
        }
        return limit;
    }

    private boolean matchesPeriod(LocalDateTime publishedAt, DiscoverPeriodType periodType) {
        if (publishedAt == null) {
            return false;
        }
        if (periodType == DiscoverPeriodType.ALL) {
            return true;
        }
        return !publishedAt.isBefore(LocalDateTime.now().minusDays(7));
    }

    private DiscoverItemView toResourceItem(ResourceItem resource, DiscoverPeriodType periodType) {
        return new DiscoverItemView(
                resource.getId(),
                DiscoverContentType.RESOURCE.name(),
                resource.getTitle(),
                resource.getSummary(),
                resourceService.findUploaderNickname(resource.getUploaderId()),
                resource.getCategory(),
                "/resources/" + resource.getId(),
                resource.getPublishedAt(),
                resourceHotScore(resource),
                periodType == DiscoverPeriodType.WEEK
                        ? "\u672c\u5468\u9ad8\u9891\u4e0b\u8f7d"
                        : "\u957f\u671f\u70ed\u95e8\u8d44\u6599");
    }

    private DiscoverItemView toPostItem(CommunityPost post, DiscoverPeriodType periodType) {
        User author = userService.findByUserId(post.getAuthorId());
        return new DiscoverItemView(
                post.getId(),
                DiscoverContentType.POST.name(),
                post.getTitle(),
                postSummary(post),
                author != null ? author.getNickname() : "Unknown User",
                postSecondaryMeta(post),
                "/community/" + post.getId(),
                post.getCreatedAt(),
                postHotScore(post, author),
                postHotLabel(post, periodType));
    }

    private DiscoverItemView toJobItem(JobPosting job, DiscoverPeriodType periodType, int favoriteCount) {
        return new DiscoverItemView(
                job.getId(),
                DiscoverContentType.JOB.name(),
                job.getTitle(),
                job.getSummary(),
                job.getCompanyName(),
                jobMetaSecondary(job),
                "/jobs/" + job.getId(),
                job.getPublishedAt(),
                jobHotScore(job, favoriteCount),
                periodType == DiscoverPeriodType.WEEK
                        ? "\u672c\u5468\u5173\u6ce8"
                        : "\u6301\u7eed\u5173\u6ce8");
    }

    private double resourceHotScore(ResourceItem resource) {
        double rawHeat = safeCount(resource.getDownloadCount()) * 2.0 + safeCount(resource.getFavoriteCount()) * 4.0;
        return rawHeat + freshnessBonus(resource.getPublishedAt());
    }

    private double postHotScore(CommunityPost post, User author) {
        double rawHeat = safeCount(post.getLikeCount()) * 3.0
                + safeCount(post.getCommentCount()) * 4.0
                + safeCount(post.getFavoriteCount()) * 5.0
                + verifiedAuthorBonus(author)
                + experiencePostBonus(post);
        return rawHeat + freshnessBonus(post.getCreatedAt());
    }

    private double jobHotScore(JobPosting job, int favoriteCount) {
        return favoriteCount * 5.0 + freshnessBonus(job.getPublishedAt());
    }

    private double freshnessBonus(LocalDateTime publishedAt) {
        if (publishedAt == null) {
            return 0;
        }
        long ageDays = Math.max(0, ChronoUnit.DAYS.between(publishedAt, LocalDateTime.now()));
        return Math.max(0, 14 - ageDays);
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private double verifiedAuthorBonus(User author) {
        return author != null && "VERIFIED".equals(author.getVerificationStatus()) ? 2.0 : 0.0;
    }

    private double experiencePostBonus(CommunityPost post) {
        return Boolean.TRUE.equals(post.getIsExperiencePost()) ? EXPERIENCE_POST_BONUS : 0.0;
    }

    private String jobMetaSecondary(JobPosting job) {
        String city = job.getCity() == null ? "" : job.getCity();
        String jobType = job.getJobType() == null ? "" : job.getJobType();
        String sourcePlatform = job.getSourcePlatform() == null ? "" : job.getSourcePlatform();
        return city + " / " + jobType + " / " + sourcePlatform;
    }

    private String postSecondaryMeta(CommunityPost post) {
        if (!Boolean.TRUE.equals(post.getIsExperiencePost())) {
            return post.getTag();
        }
        return (post.getTag() == null || post.getTag().isBlank())
                ? "Experience Post"
                : post.getTag() + " / Experience Post";
    }

    private String postSummary(CommunityPost post) {
        if (!Boolean.TRUE.equals(post.getIsExperiencePost())) {
            return abbreviate(post.getContent());
        }

        String structuredSummary = joinExperienceSummary(
                post.getExperienceTargetLabel(),
                post.getExperienceOutcomeLabel(),
                post.getExperienceTimelineSummary());
        return structuredSummary == null ? abbreviate(post.getContent()) : structuredSummary;
    }

    private String joinExperienceSummary(String targetLabel, String outcomeLabel, String timelineSummary) {
        return java.util.stream.Stream.of(targetLabel, outcomeLabel, timelineSummary)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " | " + right)
                .map(this::abbreviate)
                .orElse(null);
    }

    private String postHotLabel(CommunityPost post, DiscoverPeriodType periodType) {
        if (Boolean.TRUE.equals(post.getIsExperiencePost())) {
            return periodType == DiscoverPeriodType.WEEK ? "Weekly experience pick" : "Evergreen experience note";
        }
        return periodType == DiscoverPeriodType.WEEK
                ? "\u672c\u5468\u70ed\u8bae"
                : "\u957f\u671f\u70ed\u8bae";
    }

    private String abbreviate(String content) {
        if (content == null || content.length() <= 140) {
            return content;
        }
        return content.substring(0, 137) + "...";
    }

    private List<DiscoverItemView> mergeResults(List<DiscoverItemView> postItems,
            List<DiscoverItemView> jobItems,
            List<DiscoverItemView> resourceItems) {
        return java.util.stream.Stream.of(postItems, jobItems, resourceItems)
                .flatMap(List::stream)
                .toList();
    }

    private Comparator<DiscoverItemView> discoverComparator() {
        return Comparator.comparingDouble(DiscoverItemView::hotScore)
                .reversed()
                .thenComparing(DiscoverItemView::publishedAt, Comparator.reverseOrder())
                .thenComparing(DiscoverItemView::id, Comparator.reverseOrder());
    }
}
