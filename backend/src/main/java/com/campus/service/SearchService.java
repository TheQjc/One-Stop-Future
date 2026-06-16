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
            FallbackSearchResult fallback = fallbackSearch(normalizedQuery, normalizedType, permission);
            List<SearchResultItem> highlighted = applyManualHighlight(fallback.filteredResults, normalizedQuery);
            totals = fallback.totals;
            results = highlighted;
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

    private record FallbackSearchResult(List<SearchResultItem> filteredResults, SearchTotals totals) {
    }

    private FallbackSearchResult fallbackSearch(
            String query,
            SearchContentType type,
            PermissionContext permission) {

        List<SearchResultItem> postResults = communityService.searchPublishedPosts(query);
        List<SearchResultItem> jobResults = jobService.searchPublishedJobs(query);
        List<SearchResultItem> resourceResults = resourceService.searchPublishedResources(query);

        List<SearchResultItem> allResults = mergeResults(postResults, jobResults, resourceResults);
        List<SearchResultItem> filteredResults = filterByType(allResults, type, permission);
        SearchTotals totals = buildTotalsFromFallback(allResults);
        return new FallbackSearchResult(filteredResults, totals);
    }

    private PermissionContext buildPermissionContext(String identity) {
        if (identity == null || identity.isBlank() || "anonymousUser".equals(identity)) {
            return PermissionContext.forAnonymous();
        }
        User user = userService.findByIdentity(identity);
        return PermissionContext.fromUser(user);
    }

    private List<SearchResultItem> filterByType(
            List<SearchResultItem> allResults,
            SearchContentType type,
            PermissionContext permission) {
        return switch (type) {
            case ALL -> allResults;
            case POST -> allResults.stream().filter(r -> "POST".equals(r.type())).toList();
            case JOB -> allResults.stream().filter(r -> "JOB".equals(r.type())).toList();
            case RESOURCE -> allResults.stream().filter(r -> "RESOURCE".equals(r.type())).toList();
            case RESUME, NOTIFICATION, APPLICATION -> {
                if (permission.isAdmin() || permission.isAuthenticated()) {
                    yield allResults.stream().filter(r -> r.type().equals(type.name())).toList();
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

        String escapedQuery = Pattern.quote(escapeHtml(query.trim()));
        return escapeHtml(text).replaceAll("(?i)(" + escapedQuery + ")", preTag + "$1" + postTag);
    }

    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
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
            throw new BusinessException(400, "搜索查询词必填");
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
            throw new BusinessException(400, "无效的搜索类型");
        }
    }

    private SearchSortType normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SearchSortType.RELEVANCE;
        }
        try {
            return SearchSortType.valueOf(sort.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "无效的搜索排序方式");
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
