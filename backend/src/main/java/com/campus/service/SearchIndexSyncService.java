package com.campus.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.document.DocumentBuilder;
import com.campus.document.ElasticsearchBulkIndexer;
import com.campus.document.UnifiedSearchDocument;
import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;
import com.campus.config.SearchSyncProperties;
import com.campus.mapper.CommunityPostMapper;
import com.campus.mapper.JobPostingMapper;
import com.campus.mapper.ResourceItemMapper;
import com.campus.mapper.UserMapper;
import com.campus.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexSyncService {

    private static final String INDEX_NAME = "campus-platform";
    private static final int BATCH_SIZE = 200;

    private final ElasticsearchClient esClient;
    private final ElasticsearchBulkIndexer bulkIndexer;
    private final CommunityPostMapper communityPostMapper;
    private final JobPostingMapper jobPostingMapper;
    private final ResourceItemMapper resourceItemMapper;
    private final UserMapper userMapper;
    private final SearchSyncProperties searchSyncProperties;

    private Clock clock = Clock.systemDefaultZone();

    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>(LocalDateTime.MIN);

    public record SyncResult(int posts, int jobs, int resources, int total, long durationMs) {}

    public SyncResult fullReindex() throws IOException {
        log.info("Starting full reindex of search documents...");
        long start = System.currentTimeMillis();
        LocalDateTime syncStart = LocalDateTime.now(clock);

        ensureIndexExists();

        Map<Long, User> userMap = loadUserMap();

        int posts = indexAllPosts(userMap);
        int jobs = indexAllJobs();
        int resources = indexAllResources(userMap);

        lastSyncTime.set(syncStart);

        long duration = System.currentTimeMillis() - start;
        log.info("Full reindex completed: {} posts, {} jobs, {} resources indexed in {}ms",
                posts, jobs, resources, duration);

        return new SyncResult(posts, jobs, resources, posts + jobs + resources, duration);
    }

    public SyncResult reindexAll() throws IOException {
        deleteAndRecreateIndex();
        return fullReindex();
    }

    public SyncResult incrementalSync() throws IOException {
        LocalDateTime since = lastSyncTime.get();
        if (since.equals(LocalDateTime.MIN)) {
            reindexAll();
            return null;
        }

        log.info("Incremental sync since {}...", since);
        long start = System.currentTimeMillis();
        LocalDateTime syncStart = LocalDateTime.now(clock);

        Map<Long, User> userMap = loadUserMap();

        int posts = syncPostsSince(since, userMap);
        int jobs = syncJobsSince(since);
        int resources = syncResourcesSince(since, userMap);

        lastSyncTime.set(syncStart);

        long duration = System.currentTimeMillis() - start;
        log.info("Incremental sync completed: {} posts, {} jobs, {} resources updated in {}ms",
                posts, jobs, resources, duration);

        return new SyncResult(posts, jobs, resources, posts + jobs + resources, duration);
    }

    public SyncResult refreshUserDocuments(Long userId) throws IOException {
        if (userId == null) {
            return new SyncResult(0, 0, 0, 0, 0);
        }

        long start = System.currentTimeMillis();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return new SyncResult(0, 0, 0, 0, System.currentTimeMillis() - start);
        }

        Map<Long, User> userMap = Map.of(user.getId(), user);

        LambdaQueryWrapper<CommunityPost> postWrapper = new LambdaQueryWrapper<>();
        postWrapper.eq(CommunityPost::getAuthorId, userId)
                .eq(CommunityPost::getStatus, "PUBLISHED");
        int posts = indexPosts(communityPostMapper.selectList(postWrapper), userMap);

        LambdaQueryWrapper<ResourceItem> resourceWrapper = new LambdaQueryWrapper<>();
        resourceWrapper.eq(ResourceItem::getUploaderId, userId)
                .eq(ResourceItem::getStatus, "PUBLISHED");
        int resources = indexResources(resourceItemMapper.selectList(resourceWrapper), userMap);

        long duration = System.currentTimeMillis() - start;
        log.info("Refreshed search documents for user {}: {} posts, {} resources in {}ms",
                userId, posts, resources, duration);
        return new SyncResult(posts, 0, resources, posts + resources, duration);
    }

    private Map<Long, User> loadUserMap() {
        return userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private int indexAllPosts(Map<Long, User> userMap) throws IOException {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, "PUBLISHED");
        List<CommunityPost> posts = communityPostMapper.selectList(wrapper);
        return indexPosts(posts, userMap);
    }

    private int syncPostsSince(LocalDateTime since, Map<Long, User> userMap) throws IOException {
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(CommunityPost::getUpdatedAt, since);
        List<CommunityPost> posts = communityPostMapper.selectList(wrapper);
        deleteNonPublishedPosts(posts);
        return indexPosts(posts.stream()
                .filter(post -> "PUBLISHED".equals(post.getStatus()))
                .toList(), userMap);
    }

    private int indexPosts(List<CommunityPost> posts, Map<Long, User> userMap) throws IOException {
        List<UnifiedSearchDocument> docs = posts.stream()
                .map(post -> {
                    String authorName = userMap.get(post.getAuthorId()) != null
                            ? userMap.get(post.getAuthorId()).getNickname() : null;
                    return DocumentBuilder.fromCommunityPost(post, authorName);
                })
                .toList();
        return bulkIndex(docs);
    }

    private int indexAllJobs() throws IOException {
        LambdaQueryWrapper<JobPosting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobPosting::getStatus, "PUBLISHED");
        List<JobPosting> jobs = jobPostingMapper.selectList(wrapper);
        return indexJobs(jobs);
    }

    private int syncJobsSince(LocalDateTime since) throws IOException {
        LambdaQueryWrapper<JobPosting> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(JobPosting::getUpdatedAt, since);
        List<JobPosting> jobs = jobPostingMapper.selectList(wrapper);
        deleteNonPublishedJobs(jobs);
        return indexJobs(jobs.stream()
                .filter(job -> "PUBLISHED".equals(job.getStatus()))
                .toList());
    }

    private int indexJobs(List<JobPosting> jobs) throws IOException {
        List<UnifiedSearchDocument> docs = jobs.stream()
                .map(DocumentBuilder::fromJobPosting)
                .toList();
        return bulkIndex(docs);
    }

    private int indexAllResources(Map<Long, User> userMap) throws IOException {
        LambdaQueryWrapper<ResourceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceItem::getStatus, "PUBLISHED");
        List<ResourceItem> resources = resourceItemMapper.selectList(wrapper);
        return indexResources(resources, userMap);
    }

    private int syncResourcesSince(LocalDateTime since, Map<Long, User> userMap) throws IOException {
        LambdaQueryWrapper<ResourceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ResourceItem::getUpdatedAt, since);
        List<ResourceItem> resources = resourceItemMapper.selectList(wrapper);
        deleteNonPublishedResources(resources);
        return indexResources(resources.stream()
                .filter(resource -> "PUBLISHED".equals(resource.getStatus()))
                .toList(), userMap);
    }

    private int indexResources(List<ResourceItem> resources, Map<Long, User> userMap) throws IOException {
        List<UnifiedSearchDocument> docs = resources.stream()
                .map(resource -> {
                    String uploaderName = userMap.get(resource.getUploaderId()) != null
                            ? userMap.get(resource.getUploaderId()).getNickname() : null;
                    return DocumentBuilder.fromResourceItem(resource, uploaderName);
                })
                .toList();
        return bulkIndex(docs);
    }

    private int bulkIndex(List<UnifiedSearchDocument> docs) throws IOException {
        if (docs.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < docs.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, docs.size());
            List<UnifiedSearchDocument> batch = docs.subList(i, end);
            ElasticsearchBulkIndexer.BulkIndexResult result = bulkIndexer.bulkIndex(INDEX_NAME, batch);
            total += result.success();
        }
        return total;
    }

    private void deleteNonPublishedPosts(List<CommunityPost> posts) throws IOException {
        for (CommunityPost post : posts) {
            if (!"PUBLISHED".equals(post.getStatus())) {
                deleteDocument("post_" + post.getId());
            }
        }
    }

    private void deleteNonPublishedJobs(List<JobPosting> jobs) throws IOException {
        for (JobPosting job : jobs) {
            if (!"PUBLISHED".equals(job.getStatus())) {
                deleteDocument("job_" + job.getId());
            }
        }
    }

    private void deleteNonPublishedResources(List<ResourceItem> resources) throws IOException {
        for (ResourceItem resource : resources) {
            if (!"PUBLISHED".equals(resource.getStatus())) {
                deleteDocument("resource_" + resource.getId());
            }
        }
    }

    private void deleteDocument(String documentId) throws IOException {
        try {
            esClient.delete(DeleteRequest.of(d -> d
                    .index(INDEX_NAME)
                    .id(documentId)));
        } catch (ElasticsearchException exception) {
            if (exception.status() != 404) {
                throw exception;
            }
        }
    }

    private void ensureIndexExists() throws IOException {
        boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
        if (!exists) {
            createIndex();
        }
    }

    private void createIndex() throws IOException {
        try {
            esClient.indices().create(CreateIndexRequest.of(c -> c
                    .index(INDEX_NAME)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("0"))
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("contentType", p -> p.keyword(k -> k))
                            .properties("ownerId", p -> p.long_(l -> l))
                            .properties("visibility", p -> p.keyword(k -> k))
                            .properties("status", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t.analyzer("standard")
                                    .fields("keyword", f -> f.keyword(k -> k))))
                            .properties("content", p -> p.text(t -> t.analyzer("standard")))
                            .properties("summary", p -> p.text(t -> t.analyzer("standard")
                                    .fields("keyword", f -> f.keyword(k -> k))))
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
        } catch (ElasticsearchException exception) {
            if (!isResourceAlreadyExists(exception)) {
                throw exception;
            }
            log.info("Elasticsearch index {} already exists, continuing startup.", INDEX_NAME);
        }
    }

    private boolean isResourceAlreadyExists(ElasticsearchException exception) {
        return exception.status() == 400
                && exception.error() != null
                && "resource_already_exists_exception".equals(exception.error().type());
    }

    private void deleteAndRecreateIndex() throws IOException {
        boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
        if (exists) {
            esClient.indices().delete(DeleteIndexRequest.of(d -> d.index(INDEX_NAME)));
            log.info("Deleted existing index: {}", INDEX_NAME);
        }
        lastSyncTime.set(LocalDateTime.MIN);
    }

    @Scheduled(fixedRateString = "${app.search-sync.interval-ms:300000}")
    public void scheduledIncrementalSync() {
        if (!searchSyncProperties.isEnabled()) {
            return;
        }
        try {
            SyncResult result = incrementalSync();
            if (result != null && result.total() > 0) {
                log.info("Scheduled sync: {} documents updated in {}ms",
                        result.total(), result.durationMs());
            }
        } catch (Exception e) {
            log.error("Scheduled incremental sync failed", e);
        }
    }
}
