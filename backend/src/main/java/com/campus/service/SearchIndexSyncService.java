package com.campus.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.document.DocumentBuilder;
import com.campus.document.ElasticsearchBulkIndexer;
import com.campus.document.UnifiedSearchDocument;
import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;
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

    private final AtomicReference<LocalDateTime> lastSyncTime = new AtomicReference<>(LocalDateTime.MIN);

    public record SyncResult(int posts, int jobs, int resources, int total, long durationMs) {}

    public SyncResult fullReindex() throws IOException {
        log.info("Starting full reindex of search documents...");
        long start = System.currentTimeMillis();

        ensureIndexExists();

        Map<Long, User> userMap = loadUserMap();

        int posts = indexAllPosts(userMap);
        int jobs = indexAllJobs();
        int resources = indexAllResources(userMap);

        lastSyncTime.set(LocalDateTime.now());

        long duration = System.currentTimeMillis() - start;
        log.info("Full reindex completed: {} posts, {} jobs, {} resources indexed in {}ms",
                posts, jobs, resources, duration);

        return new SyncResult(posts, jobs, resources, posts + jobs + resources, duration);
    }

    public void reindexAll() throws IOException {
        deleteAndRecreateIndex();
        fullReindex();
    }

    public SyncResult incrementalSync() throws IOException {
        LocalDateTime since = lastSyncTime.get();
        if (since.equals(LocalDateTime.MIN)) {
            fullReindex();
            return null;
        }

        log.info("Incremental sync since {}...", since);
        long start = System.currentTimeMillis();

        Map<Long, User> userMap = loadUserMap();

        int posts = syncPostsSince(since, userMap);
        int jobs = syncJobsSince(since);
        int resources = syncResourcesSince(since, userMap);

        lastSyncTime.set(LocalDateTime.now());

        long duration = System.currentTimeMillis() - start;
        log.info("Incremental sync completed: {} posts, {} jobs, {} resources updated in {}ms",
                posts, jobs, resources, duration);

        return new SyncResult(posts, jobs, resources, posts + jobs + resources, duration);
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
        wrapper.eq(CommunityPost::getStatus, "PUBLISHED")
                .ge(CommunityPost::getUpdatedAt, since);
        List<CommunityPost> posts = communityPostMapper.selectList(wrapper);
        return indexPosts(posts, userMap);
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
        wrapper.eq(JobPosting::getStatus, "PUBLISHED")
                .ge(JobPosting::getUpdatedAt, since);
        List<JobPosting> jobs = jobPostingMapper.selectList(wrapper);
        return indexJobs(jobs);
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
        wrapper.eq(ResourceItem::getStatus, "PUBLISHED")
                .ge(ResourceItem::getUpdatedAt, since);
        List<ResourceItem> resources = resourceItemMapper.selectList(wrapper);
        return indexResources(resources, userMap);
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

    private void ensureIndexExists() throws IOException {
        boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
        if (!exists) {
            log.info("Index {} does not exist, triggering full reindex to create it", INDEX_NAME);
            fullReindex();
        }
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
