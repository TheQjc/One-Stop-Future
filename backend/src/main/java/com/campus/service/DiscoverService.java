package com.campus.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.DiscoverContentType;
import com.campus.common.DiscoverPeriodType;
import com.campus.dto.DiscoverItemView;
import com.campus.dto.DiscoverResponse;
import com.campus.entity.ResourceItem;

@Service
public class DiscoverService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final ResourceService resourceService;

    public DiscoverService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public DiscoverResponse discover(String tab, String period, Integer limit) {
        DiscoverContentType normalizedTab = normalizeTab(tab);
        DiscoverPeriodType normalizedPeriod = normalizePeriod(period);
        int normalizedLimit = normalizeLimit(limit);

        List<DiscoverItemView> resourceItems = resourceService.listPublishedDiscoverResources().stream()
                .filter(resource -> matchesPeriod(resource.getPublishedAt(), normalizedPeriod))
                .map(resource -> toResourceItem(resource, normalizedPeriod))
                .sorted(discoverComparator())
                .limit(normalizedLimit)
                .toList();

        List<DiscoverItemView> items = switch (normalizedTab) {
            case ALL, RESOURCE -> resourceItems;
            case POST, JOB -> List.of();
        };

        return new DiscoverResponse(
                normalizedTab.name(),
                normalizedPeriod.name(),
                items.size(),
                items);
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

    private double resourceHotScore(ResourceItem resource) {
        double rawHeat = safeCount(resource.getDownloadCount()) * 2.0 + safeCount(resource.getFavoriteCount()) * 4.0;
        return rawHeat + freshnessBonus(resource.getPublishedAt());
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

    private Comparator<DiscoverItemView> discoverComparator() {
        return Comparator.comparingDouble(DiscoverItemView::hotScore)
                .reversed()
                .thenComparing(DiscoverItemView::publishedAt, Comparator.reverseOrder())
                .thenComparing(DiscoverItemView::id, Comparator.reverseOrder());
    }
}
