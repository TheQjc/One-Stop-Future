package com.campus.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.PermissionContext;
import com.campus.common.SearchContentType;
import com.campus.common.SearchSortType;
import com.campus.config.ElasticsearchIntegrationProperties;
import com.campus.dto.SearchResponse;
import com.campus.dto.SearchResponse.SearchResultItem;
import com.campus.dto.SearchResponse.SearchTotals;
import com.campus.entity.User;

@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final CommunityService communityService;
    private final JobService jobService;
    private final ResourceService resourceService;
    private final UnifiedSearchService unifiedSearchService;
    private final UserService userService;
    private final ElasticsearchIntegrationProperties esProperties;

    public SearchService(CommunityService communityService, JobService jobService, ResourceService resourceService,
            UnifiedSearchService unifiedSearchService, UserService userService,
            ElasticsearchIntegrationProperties esProperties) {
        this.communityService = communityService;
        this.jobService = jobService;
        this.resourceService = resourceService;
        this.unifiedSearchService = unifiedSearchService;
        this.userService = userService;
        this.esProperties = esProperties;
    }

    public SearchResponse search(String query, String type, String sort, String identity) {
        String normalizedQuery = normalizeRequiredQuery(query);
        SearchContentType normalizedType = normalizeType(type);
        SearchSortType normalizedSort = normalizeSort(sort);
        PermissionContext permission = buildPermissionContext(identity);

        UnifiedSearchService.SearchResult esResult =
                unifiedSearchService.search(normalizedQuery, normalizedType, normalizedSort, permission, 0, 50);

        List<SearchResultItem> results;
        SearchTotals totals;

        if (esResult.degraded()) {
            log.warn("Elasticsearch unavailable, falling back to MySQL search for query: {}", normalizedQuery);
            results = fallbackSearch(normalizedQuery, normalizedType, normalizedSort, permission);
            results = applyManualHighlight(results, normalizedQuery);
            totals = buildTotalsFromFallback(results);
        } else {
            results = esResult.items();
            totals = buildTotalsFromEs(esResult.typeCounts());
        }

        results = sortResults(results, normalizedQuery, normalizedSort);

        return new SearchResponse(
                normalizedQuery,
                normalizedType.name(),
                normalizedSort.name(),
                totals,
                results);
    }

    private PermissionContext buildPermissionContext(String identity) {
        if (identity == null || identity.isBlank() || "anonymousUser".equals(identity)) {
            return PermissionContext.forAnonymous();
        }
        User user = userService.findByIdentity(identity);
        return PermissionContext.fromUser(user);
    }

    private List<SearchResultItem> fallbackSearch(
            String query,
            SearchContentType type,
            SearchSortType sort,
            PermissionContext permission) {

        List<SearchResultItem> postResults = communityService.searchPublishedPosts(query);
        List<SearchResultItem> jobResults = jobService.searchPublishedJobs(query);
        List<SearchResultItem> resourceResults = resourceService.searchPublishedResources(query);

        return switch (type) {
            case ALL -> mergeResults(postResults, jobResults, resourceResults);
            case POST -> postResults;
            case JOB -> jobResults;
            case RESOURCE -> resourceResults;
            case RESUME, NOTIFICATION, APPLICATION -> {
                if (permission.isAdmin() || permission.isAuthenticated()) {
                    yield List.of();
                }
                yield List.of();
            }
        };
    }

    private List<SearchResultItem> applyManualHighlight(List<SearchResultItem> results, String query) {
        if (results == null || results.isEmpty() || query == null || query.isBlank()) {
            return results;
        }

        String preTag = esProperties.getHighlight().getPreTag();
        String postTag = esProperties.getHighlight().getPostTag();

        return results.stream().map(item -> {
            String highlightedTitle = highlightText(item.title(), query, preTag, postTag);
            String highlightedSummary = highlightText(item.summary(), query, preTag, postTag);
            String highlightedContent = null;

            return new SearchResultItem(
                    item.id(),
                    item.type(),
                    item.title(),
                    item.summary(),
                    item.metaPrimary(),
                    item.metaSecondary(),
                    item.path(),
                    item.publishedAt(),
                    highlightedTitle,
                    highlightedSummary,
                    highlightedContent
            );
        }).toList();
    }

    private String highlightText(String text, String query, String preTag, String postTag) {
        if (text == null || text.isBlank() || query == null || query.isBlank()) {
            return null;
        }

        String escapedQuery = Pattern.quote(query.trim());
        return text.replaceAll("(?i)(" + escapedQuery + ")", preTag + "$1" + postTag);
    }

    private List<SearchResultItem> mergeResults(
            List<SearchResultItem> postResults,
            List<SearchResultItem> jobResults,
            List<SearchResultItem> resourceResults) {
        return java.util.stream.Stream.of(postResults, jobResults, resourceResults)
                .flatMap(List::stream)
                .toList();
    }

    private SearchTotals buildTotalsFromFallback(List<SearchResultItem> results) {
        Map<String, Long> counts = new HashMap<>();
        for (SearchResultItem item : results) {
            String key = item.type().toLowerCase();
            counts.merge(key, 1L, Long::sum);
        }
        return buildTotalsFromEs(counts);
    }

    private SearchTotals buildTotalsFromEs(Map<String, Long> typeCounts) {
        int all = typeCounts.values().stream().mapToInt(Long::intValue).sum();
        int post = typeCounts.getOrDefault("post", 0L).intValue();
        int job = typeCounts.getOrDefault("job", 0L).intValue();
        int resource = typeCounts.getOrDefault("resource", 0L).intValue();
        int resume = typeCounts.getOrDefault("resume", 0L).intValue();
        int notification = typeCounts.getOrDefault("notification", 0L).intValue();
        int application = typeCounts.getOrDefault("application", 0L).intValue();
        return new SearchTotals(all, post, job, resource, resume, notification, application);
    }

    private String normalizeRequiredQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(400, "search query is required");
        }
        return query.trim();
    }

    private SearchContentType normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return SearchContentType.ALL;
        }
        try {
            return SearchContentType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid search type");
        }
    }

    private SearchSortType normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SearchSortType.RELEVANCE;
        }
        try {
            return SearchSortType.valueOf(sort.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid search sort");
        }
    }

    private List<SearchResultItem> sortResults(List<SearchResultItem> results,
            String normalizedQuery, SearchSortType sortType) {
        Comparator<SearchResultItem> comparator = switch (sortType) {
            case RELEVANCE -> relevanceComparator(normalizedQuery);
            case LATEST -> latestComparator();
        };
        return results.stream()
                .sorted(comparator)
                .toList();
    }

    private Comparator<SearchResultItem> relevanceComparator(String normalizedQuery) {
        return Comparator
                .comparingInt((SearchResultItem item) -> relevanceBucket(item, normalizedQuery))
                .thenComparing(SearchResultItem::publishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SearchResultItem::id, Comparator.reverseOrder());
    }

    private Comparator<SearchResultItem> latestComparator() {
        return Comparator
                .comparing(SearchResultItem::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SearchResultItem::id, Comparator.reverseOrder());
    }

    private int relevanceBucket(SearchResultItem item, String normalizedQuery) {
        String lowerQuery = normalizedQuery.toLowerCase(Locale.ROOT);
        if (containsIgnoreCase(item.title(), lowerQuery)) {
            return 0;
        }
        if (containsIgnoreCase(item.summary(), lowerQuery)
                || containsIgnoreCase(item.metaPrimary(), lowerQuery)
                || containsIgnoreCase(item.metaSecondary(), lowerQuery)) {
            return 1;
        }
        return 2;
    }

    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }
}
