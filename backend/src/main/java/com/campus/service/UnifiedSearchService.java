package com.campus.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.JsonData;
import com.campus.common.PermissionContext;
import com.campus.common.SearchContentType;
import com.campus.common.SearchSortType;
import com.campus.config.ElasticsearchIntegrationProperties;
import com.campus.document.UnifiedSearchDocument;
import com.campus.dto.SearchResponse.SearchResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedSearchService {

    private static final String INDEX_NAME = "campus-platform";

    private static final List<String> SEARCHABLE_FIELDS = List.of(
            "title^3", "content^2", "summary", "authorName", "tags"
    );

    private static final Set<SearchContentType> PUBLIC_TYPES = EnumSet.of(
            SearchContentType.POST, SearchContentType.JOB, SearchContentType.RESOURCE
    );

    private static final Set<SearchContentType> PRIVATE_TYPES = EnumSet.of(
            SearchContentType.RESUME, SearchContentType.NOTIFICATION, SearchContentType.APPLICATION
    );

    private final ElasticsearchClient esClient;
    private final ElasticsearchIntegrationProperties esProperties;

    public record SearchResult(
            List<SearchResultItem> items,
            Map<String, Long> typeCounts,
            long totalHits,
            boolean degraded,
            Map<String, String> highlights) {
    }

    public SearchResult search(
            String query,
            SearchContentType contentType,
            SearchSortType sortType,
            PermissionContext permission,
            int page,
            int pageSize) {

        boolean degraded = false;
        List<SearchResultItem> items = new ArrayList<>();
        Map<String, Long> typeCounts = new java.util.HashMap<>();
        long totalHits = 0;
        Map<String, String> highlights = Map.of();

        try {
            ensureIndexExists();
            SearchResponse<UnifiedSearchDocument> response = executeSearch(query, contentType, sortType, permission, page, pageSize);
            totalHits = response.hits().total() != null ? response.hits().total().value() : 0;

            for (Hit<UnifiedSearchDocument> hit : response.hits().hits()) {
                UnifiedSearchDocument doc = hit.source();
                if (doc == null) continue;

                Map<String, List<String>> hitHighlights = hit.highlight();
                String highlightedTitle = extractHighlight(hitHighlights, "title");
                String highlightedSummary = extractHighlight(hitHighlights, "summary");
                String highlightedContent = extractHighlight(hitHighlights, "content");

                String summary = highlightedSummary != null ? highlightedSummary
                        : (doc.getSummary() != null ? doc.getSummary() : "");

                SearchResultItem item = new SearchResultItem(
                        extractDocId(doc.getId()),
                        doc.getContentType().toUpperCase(),
                        doc.getTitle(),
                        summary,
                        doc.getAuthorName(),
                        null,
                        doc.getPath(),
                        doc.getPublishedAt(),
                        highlightedTitle,
                        highlightedSummary,
                        highlightedContent
                );

                items.add(item);

                String typeKey = doc.getContentType().toLowerCase();
                typeCounts.merge(typeKey, 1L, Long::sum);
            }

            if (!items.isEmpty()) {
                highlights = Map.of(
                        "preTag", esProperties.getHighlight().getPreTag(),
                        "postTag", esProperties.getHighlight().getPostTag()
                );
            }

        } catch (IOException e) {
            log.error("Elasticsearch search failed, will fallback to database", e);
            degraded = true;
        }

        return new SearchResult(items, typeCounts, totalHits, degraded, highlights);
    }

    private SearchResponse<UnifiedSearchDocument> executeSearch(
            String query,
            SearchContentType contentType,
            SearchSortType sortType,
            PermissionContext permission,
            int page,
            int pageSize) throws IOException {

        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        if (StringUtils.hasText(query)) {
            MultiMatchQuery multiMatch = MultiMatchQuery.of(mm -> mm
                    .query(query)
                    .fields(SEARCHABLE_FIELDS)
                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
            );
            boolQuery.must(Query.of(q -> q.multiMatch(multiMatch)));
        }

        List<Query> typeFilters = buildTypeFilters(contentType, permission);
        if (!typeFilters.isEmpty()) {
            for (Query filter : typeFilters) {
                boolQuery.filter(filter);
            }
        }

        boolQuery.filter(Query.of(q -> q.term(TermQuery.of(t -> t.field("visibility").value("public")))));

        if (!permission.isAdmin()) {
            if (permission.isAuthenticated()) {
                boolQuery.should(Query.of(q -> q.bool(b -> b
                        .filter(Query.of(fq -> fq.term(TermQuery.of(t -> t.field("visibility").value("private")))))
                        .filter(Query.of(fq -> fq.term(TermQuery.of(t -> t.field("ownerId").value(permission.getUserId()))))))));
            }
        }

        Highlight highlight = Highlight.of(h -> h
                .fields("title", HighlightField.of(hf -> hf
                        .preTags(esProperties.getHighlight().getPreTag())
                        .postTags(esProperties.getHighlight().getPostTag())
                        .numberOfFragments(0)
                        .fragmentSize(0)))
                .fields("summary", HighlightField.of(hf -> hf
                        .preTags(esProperties.getHighlight().getPreTag())
                        .postTags(esProperties.getHighlight().getPostTag())
                        .numberOfFragments(0)
                        .fragmentSize(0)))
                .fields("content", HighlightField.of(hf -> hf
                        .preTags(esProperties.getHighlight().getPreTag())
                        .postTags(esProperties.getHighlight().getPostTag())
                        .numberOfFragments(0)
                        .fragmentSize(0)))
        );

        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .query(Query.of(q -> q.bool(boolQuery.build())))
                .highlight(highlight)
                .from(page * pageSize)
                .size(pageSize);

        if (sortType == SearchSortType.LATEST) {
            requestBuilder.sort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)));
        }

        return esClient.search(requestBuilder.build(), UnifiedSearchDocument.class);
    }

    private List<Query> buildTypeFilters(SearchContentType contentType, PermissionContext permission) {
        List<Query> filters = new ArrayList<>();

        if (contentType == null || contentType.isAll()) {
            List<String> allowedTypes = new ArrayList<>();
            allowedTypes.add(SearchContentType.POST.code());
            allowedTypes.add(SearchContentType.JOB.code());
            allowedTypes.add(SearchContentType.RESOURCE.code());

            if (permission.isAuthenticated() || permission.isAdmin()) {
                allowedTypes.add(SearchContentType.RESUME.code());
                allowedTypes.add(SearchContentType.NOTIFICATION.code());
                allowedTypes.add(SearchContentType.APPLICATION.code());
            }

            filters.add(Query.of(q -> q.terms(t -> t
                    .field("contentType")
                    .terms(tv -> tv.value(allowedTypes.stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                            .toList())))));
        } else {
            filters.add(Query.of(q -> q.term(TermQuery.of(t -> t.field("contentType").value(contentType.code())))));
        }

        return filters;
    }

    private String extractHighlight(Map<String, List<String>> highlights, String field) {
        if (highlights == null || !highlights.containsKey(field)) {
            return null;
        }
        List<String> fragments = highlights.get(field);
        if (fragments == null || fragments.isEmpty()) {
            return null;
        }
        return String.join("... ", fragments);
    }

    private Long extractDocId(String docId) {
        if (docId == null) return null;
        String[] parts = docId.split("_", 2);
        if (parts.length < 2) {
            try {
                return Long.parseLong(parts[0]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void ensureIndexExists() {
        try {
            boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
            if (!exists) {
                createIndex();
            }
        } catch (IOException e) {
            log.error("Failed to check/create ES index", e);
        }
    }

    private void createIndex() {
        try {
            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("0")
                    )                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("contentType", p -> p.keyword(k -> k))
                            .properties("ownerId", p -> p.long_(l -> l))
                            .properties("visibility", p -> p.keyword(k -> k))
                            .properties("status", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t.analyzer("standard").fields("keyword", f -> f.keyword(k -> k))))
                            .properties("content", p -> p.text(t -> t.analyzer("standard")))
                            .properties("summary", p -> p.text(t -> t.analyzer("standard").fields("keyword", f -> f.keyword(k -> k))))
                            .properties("tags", p -> p.keyword(k -> k))
                            .properties("authorName", p -> p.text(t -> t.fields("keyword", f -> f.keyword(k -> k))))
                            .properties("authorId", p -> p.long_(l -> l))
                            .properties("publishedAt", p -> p.date(d -> d))
                            .properties("createdAt", p -> p.date(d -> d))
                            .properties("path", p -> p.keyword(k -> k))
                            .properties("extra", p -> p.object(o -> o.enabled(true)))
                    )
            ));
            log.info("Created Elasticsearch index: {}", INDEX_NAME);
        } catch (IOException e) {
            log.error("Failed to create ES index", e);
        }
    }
}
