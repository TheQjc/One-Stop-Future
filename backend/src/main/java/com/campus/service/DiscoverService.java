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

@Service
public class DiscoverService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final CommunityService communityService;
    private final JobService jobService;
    private final ResourceService resourceService;
    private final UserService userService;

    public DiscoverService(CommunityService communityService, JobService jobService, ResourceService resourceService,
            UserService userService) {
        this.communityService = communityService;
        this.jobService = jobService;
        this.resourceService = resourceService;
        this.userService = userService;
    }

    public DiscoverResponse discover(String tab, String period, Integer limit) {
        DiscoverContentType normalizedTab = normalizeTab(tab);
        DiscoverPeriodType normalizedPeriod = normalizePeriod(period);
        int normalizedLimit = normalizeLimit(limit);

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
                periodType == DiscoverPeriodType.WEEK ? "本周高频下载" : "长期热门资料");
    }

    private DiscoverItemView toPostItem(CommunityPost post, DiscoverPeriodType periodType) {
        User author = userService.findByUserId(post.getAuthorId());
        return new DiscoverItemView(
                post.getId(),
                DiscoverContentType.POST.name(),
                post.getTitle(),
                abbreviate(post.getContent()),
                author != null ? author.getNickname() : "Unknown User",
                post.getTag(),
                "/community/" + post.getId(),
                post.getCreatedAt(),
                postHotScore(post, author),
                periodType == DiscoverPeriodType.WEEK ? "本周热议" : "长期热议");
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
                periodType == DiscoverPeriodType.WEEK ? "本周关注" : "持续关注");
    }

    private double resourceHotScore(ResourceItem resource) {
        double rawHeat = safeCount(resource.getDownloadCount()) * 2.0 + safeCount(resource.getFavoriteCount()) * 4.0;
        return rawHeat + freshnessBonus(resource.getPublishedAt());
    }

    private double postHotScore(CommunityPost post, User author) {
        double rawHeat = safeCount(post.getLikeCount()) * 3.0
                + safeCount(post.getCommentCount()) * 4.0
                + safeCount(post.getFavoriteCount()) * 5.0
                + verifiedAuthorBonus(author);
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

    private String jobMetaSecondary(JobPosting job) {
        String city = job.getCity() == null ? "" : job.getCity();
        String jobType = job.getJobType() == null ? "" : job.getJobType();
        String sourcePlatform = job.getSourcePlatform() == null ? "" : job.getSourcePlatform();
        return city + " / " + jobType + " / " + sourcePlatform;
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
