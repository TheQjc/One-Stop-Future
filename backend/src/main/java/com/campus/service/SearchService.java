package com.campus.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.SearchContentType;
import com.campus.common.SearchSortType;
import com.campus.dto.SearchResponse;

@Service
public class SearchService {

    private final CommunityService communityService;
    private final JobService jobService;
    private final ResourceService resourceService;

    public SearchService(CommunityService communityService, JobService jobService, ResourceService resourceService) {
        this.communityService = communityService;
        this.jobService = jobService;
        this.resourceService = resourceService;
    }

    public SearchResponse search(String query, String type, String sort, String identity) {
        String normalizedQuery = normalizeRequiredQuery(query);
        SearchContentType normalizedType = normalizeType(type);
        SearchSortType normalizedSort = normalizeSort(sort);

        List<SearchResponse.SearchResultItem> postResults = communityService.searchPublishedPosts(normalizedQuery);
        List<SearchResponse.SearchResultItem> jobResults = jobService.searchPublishedJobs(normalizedQuery);
        List<SearchResponse.SearchResultItem> resourceResults = resourceService.searchPublishedResources(normalizedQuery);
        SearchResponse.SearchTotals totals = new SearchResponse.SearchTotals(
                postResults.size() + jobResults.size() + resourceResults.size(),
                postResults.size(),
                jobResults.size(),
                resourceResults.size());

        List<SearchResponse.SearchResultItem> filteredResults = sortResults(switch (normalizedType) {
            case ALL -> mergeResults(postResults, jobResults, resourceResults);
            case POST -> postResults;
            case JOB -> jobResults;
            case RESOURCE -> resourceResults;
        }, normalizedQuery, normalizedSort);

        return new SearchResponse(
                normalizedQuery,
                normalizedType.name(),
                normalizedSort.name(),
                totals,
                filteredResults);
    }

    private List<SearchResponse.SearchResultItem> mergeResults(List<SearchResponse.SearchResultItem> postResults,
            List<SearchResponse.SearchResultItem> jobResults, List<SearchResponse.SearchResultItem> resourceResults) {
        return java.util.stream.Stream.of(postResults, jobResults, resourceResults)
                .flatMap(List::stream)
                .toList();
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

    private List<SearchResponse.SearchResultItem> sortResults(List<SearchResponse.SearchResultItem> results,
            String normalizedQuery, SearchSortType sortType) {
        Comparator<SearchResponse.SearchResultItem> comparator = switch (sortType) {
            case RELEVANCE -> relevanceComparator(normalizedQuery);
            case LATEST -> latestComparator();
        };
        return results.stream()
                .sorted(comparator)
                .toList();
    }

    private Comparator<SearchResponse.SearchResultItem> relevanceComparator(String normalizedQuery) {
        return Comparator
                .comparingInt((SearchResponse.SearchResultItem item) -> relevanceBucket(item, normalizedQuery))
                .thenComparing(SearchResponse.SearchResultItem::publishedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SearchResponse.SearchResultItem::id, Comparator.reverseOrder());
    }

    private Comparator<SearchResponse.SearchResultItem> latestComparator() {
        return Comparator
                .comparing(SearchResponse.SearchResultItem::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SearchResponse.SearchResultItem::id, Comparator.reverseOrder());
    }

    private int relevanceBucket(SearchResponse.SearchResultItem item, String normalizedQuery) {
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
