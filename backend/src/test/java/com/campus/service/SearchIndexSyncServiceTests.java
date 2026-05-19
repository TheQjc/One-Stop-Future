package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.config.SearchSyncProperties;
import com.campus.document.ElasticsearchBulkIndexer;
import com.campus.document.UnifiedSearchDocument;
import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;
import com.campus.mapper.CommunityPostMapper;
import com.campus.mapper.JobPostingMapper;
import com.campus.mapper.ResourceItemMapper;
import com.campus.mapper.UserMapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;

class SearchIndexSyncServiceTests {

    @Test
    void scheduledIncrementalSyncSkipsWhileAnotherSyncIsRunning() throws Exception {
        SearchIndexSyncService syncService = org.mockito.Mockito.spy(new SearchIndexSyncService(
                null,
                null,
                null,
                null,
                null,
                null,
                new SearchSyncProperties()));
        org.mockito.Mockito.doReturn(new SearchIndexSyncService.SyncResult(1, 0, 0, 1, 1))
                .when(syncService)
                .incrementalSync();

        ReentrantLock syncLock = (ReentrantLock) ReflectionTestUtils.getField(syncService, "syncLock");
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            syncLock.lock();
            try {
                lockHeld.countDown();
                assertThat(releaseLock.await(5, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                syncLock.unlock();
            }
        });
        holder.start();
        assertThat(lockHeld.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            syncService.scheduledIncrementalSync();
        } finally {
            releaseLock.countDown();
            holder.join(5000);
        }

        org.mockito.Mockito.verify(syncService, org.mockito.Mockito.never()).incrementalSync();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullReindexIgnoresCreateIndexRaceWhenIndexAlreadyExists() throws Exception {
        ElasticsearchClient esClient = org.mockito.Mockito.mock(ElasticsearchClient.class);
        ElasticsearchIndicesClient indicesClient = org.mockito.Mockito.mock(ElasticsearchIndicesClient.class);
        ElasticsearchBulkIndexer bulkIndexer = org.mockito.Mockito.mock(ElasticsearchBulkIndexer.class);
        CommunityPostMapper communityPostMapper = org.mockito.Mockito.mock(CommunityPostMapper.class);
        JobPostingMapper jobPostingMapper = org.mockito.Mockito.mock(JobPostingMapper.class);
        ResourceItemMapper resourceItemMapper = org.mockito.Mockito.mock(ResourceItemMapper.class);
        UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);

        when(esClient.indices()).thenReturn(indicesClient);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(new BooleanResponse(false));
        when(indicesClient.create(any(CreateIndexRequest.class))).thenThrow(elasticsearchException(
                400,
                "resource_already_exists_exception",
                "index [campus-platform] already exists"));
        when(userMapper.selectList(null)).thenReturn(List.of());
        when(communityPostMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(jobPostingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(resourceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        SearchIndexSyncService syncService = new SearchIndexSyncService(
                esClient,
                bulkIndexer,
                communityPostMapper,
                jobPostingMapper,
                resourceItemMapper,
                userMapper,
                new SearchSyncProperties());

        SearchIndexSyncService.SyncResult result = syncService.fullReindex();

        assertThat(result.total()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void incrementalSyncDeletesDocumentsForResourcesThatAreNoLongerPublished() throws Exception {
        ElasticsearchClient esClient = org.mockito.Mockito.mock(ElasticsearchClient.class);
        ElasticsearchBulkIndexer bulkIndexer = org.mockito.Mockito.mock(ElasticsearchBulkIndexer.class);
        CommunityPostMapper communityPostMapper = org.mockito.Mockito.mock(CommunityPostMapper.class);
        JobPostingMapper jobPostingMapper = org.mockito.Mockito.mock(JobPostingMapper.class);
        ResourceItemMapper resourceItemMapper = org.mockito.Mockito.mock(ResourceItemMapper.class);
        UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);

        ResourceItem offlineResource = new ResourceItem();
        offlineResource.setId(42L);
        offlineResource.setStatus("OFFLINE");
        offlineResource.setUpdatedAt(LocalDateTime.now());

        when(userMapper.selectList(null)).thenReturn(List.of());
        when(communityPostMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(jobPostingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(resourceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(offlineResource));

        SearchIndexSyncService syncService = new SearchIndexSyncService(
                esClient,
                bulkIndexer,
                communityPostMapper,
                jobPostingMapper,
                resourceItemMapper,
                userMapper,
                new SearchSyncProperties());
        AtomicReference<LocalDateTime> lastSyncTime =
                (AtomicReference<LocalDateTime>) ReflectionTestUtils.getField(syncService, "lastSyncTime");
        lastSyncTime.set(LocalDateTime.now().minusMinutes(5));

        syncService.incrementalSync();

        ArgumentCaptor<DeleteRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteRequest.class);
        verify(esClient).delete(deleteRequestCaptor.capture());
        assertThat(deleteRequestCaptor.getValue().index()).isEqualTo("campus-platform");
        assertThat(deleteRequestCaptor.getValue().id()).isEqualTo("resource_42");
    }

    @Test
    @SuppressWarnings("unchecked")
    void incrementalSyncKeepsTheStartWatermarkSoUpdatesDuringRunAreNotSkipped() throws Exception {
        ElasticsearchClient esClient = org.mockito.Mockito.mock(ElasticsearchClient.class);
        ElasticsearchBulkIndexer bulkIndexer = org.mockito.Mockito.mock(ElasticsearchBulkIndexer.class);
        CommunityPostMapper communityPostMapper = org.mockito.Mockito.mock(CommunityPostMapper.class);
        JobPostingMapper jobPostingMapper = org.mockito.Mockito.mock(JobPostingMapper.class);
        ResourceItemMapper resourceItemMapper = org.mockito.Mockito.mock(ResourceItemMapper.class);
        UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);

        CountDownLatch bulkIndexStarted = new CountDownLatch(1);
        CountDownLatch allowBulkIndex = new CountDownLatch(1);
        when(bulkIndexer.bulkIndex(anyString(), anyList())).thenAnswer(invocation -> {
            bulkIndexStarted.countDown();
            assertThat(allowBulkIndex.await(5, TimeUnit.SECONDS)).isTrue();
            @SuppressWarnings("unchecked")
            List<UnifiedSearchDocument> documents = invocation.getArgument(1);
            return new ElasticsearchBulkIndexer.BulkIndexResult(documents.size(), 0, List.of());
        });
        when(communityPostMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(jobPostingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(userMapper.selectList(null)).thenReturn(List.of());

        ResourceItem sentinelResource = new ResourceItem();
        sentinelResource.setId(7L);
        sentinelResource.setStatus("PUBLISHED");
        sentinelResource.setUpdatedAt(LocalDateTime.now().minusMinutes(10));

        ResourceItem updatedResource = new ResourceItem();
        updatedResource.setId(42L);
        updatedResource.setStatus("PUBLISHED");
        LocalDateTime previousSyncTime = LocalDateTime.of(2026, 5, 19, 8, 0);
        LocalDateTime syncStartTime = LocalDateTime.of(2026, 5, 19, 9, 0);
        LocalDateTime updatedDuringSync = LocalDateTime.of(2026, 5, 19, 9, 30);
        updatedResource.setUpdatedAt(previousSyncTime.minusHours(1));

        AtomicReference<SearchIndexSyncService> serviceRef = new AtomicReference<>();
        when(resourceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            if (serviceRef.get() == null) {
                return List.of();
            }
            if (allowBulkIndex.getCount() == 1) {
                return List.of(sentinelResource);
            }
            AtomicReference<LocalDateTime> watermark =
                    (AtomicReference<LocalDateTime>) ReflectionTestUtils.getField(serviceRef.get(), "lastSyncTime");
            return watermark.get().isBefore(updatedResource.getUpdatedAt()) ? List.of(updatedResource) : List.of();
        });

        SearchIndexSyncService syncService = new SearchIndexSyncService(
                esClient,
                bulkIndexer,
                communityPostMapper,
                jobPostingMapper,
                resourceItemMapper,
                userMapper,
                new SearchSyncProperties());
        ReflectionTestUtils.setField(syncService, "clock", Clock.fixed(
                syncStartTime.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
        serviceRef.set(syncService);
        AtomicReference<LocalDateTime> lastSyncTime =
                (AtomicReference<LocalDateTime>) ReflectionTestUtils.getField(syncService, "lastSyncTime");
        lastSyncTime.set(previousSyncTime);

        CompletableFuture<Void> firstRun = CompletableFuture.runAsync(() -> {
            try {
                syncService.incrementalSync();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(bulkIndexStarted.await(5, TimeUnit.SECONDS)).isTrue();
        updatedResource.setUpdatedAt(updatedDuringSync);
        allowBulkIndex.countDown();
        firstRun.get(5, TimeUnit.SECONDS);

        syncService.incrementalSync();

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bulkIndexer, times(2)).bulkIndex(eq("campus-platform"), docsCaptor.capture());
        assertThat(docsCaptor.getAllValues())
                .hasSize(2);
        assertThat(docsCaptor.getAllValues().get(0))
                .extracting(doc -> ((UnifiedSearchDocument) doc).getId())
                .containsExactly("resource_7");
        assertThat(docsCaptor.getAllValues().get(1))
                .extracting(doc -> ((UnifiedSearchDocument) doc).getId())
                .containsExactly("resource_42");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshUserDocumentsReindexesPublishedPostsAndResourcesWithLatestNickname() throws Exception {
        ElasticsearchClient esClient = org.mockito.Mockito.mock(ElasticsearchClient.class);
        ElasticsearchBulkIndexer bulkIndexer = org.mockito.Mockito.mock(ElasticsearchBulkIndexer.class);
        CommunityPostMapper communityPostMapper = org.mockito.Mockito.mock(CommunityPostMapper.class);
        JobPostingMapper jobPostingMapper = org.mockito.Mockito.mock(JobPostingMapper.class);
        ResourceItemMapper resourceItemMapper = org.mockito.Mockito.mock(ResourceItemMapper.class);
        UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);

        when(bulkIndexer.bulkIndex(anyString(), anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<UnifiedSearchDocument> documents = invocation.getArgument(1);
            return new ElasticsearchBulkIndexer.BulkIndexResult(documents.size(), 0, List.of());
        });

        com.campus.entity.User user = new com.campus.entity.User();
        user.setId(2L);
        user.setNickname("FutureRunner");
        when(userMapper.selectById(2L)).thenReturn(user);

        CommunityPost post = new CommunityPost();
        post.setId(10L);
        post.setAuthorId(2L);
        post.setTitle("Exam planning checklist");
        post.setContent("Plan backward from exam week");
        post.setStatus("PUBLISHED");
        post.setCreatedAt(LocalDateTime.now());

        ResourceItem resource = new ResourceItem();
        resource.setId(20L);
        resource.setUploaderId(2L);
        resource.setTitle("Resume Template");
        resource.setSummary("One-page template");
        resource.setStatus("PUBLISHED");
        resource.setPublishedAt(LocalDateTime.now());
        resource.setCreatedAt(LocalDateTime.now());

        when(communityPostMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(post));
        when(resourceItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(resource));

        SearchIndexSyncService syncService = new SearchIndexSyncService(
                esClient,
                bulkIndexer,
                communityPostMapper,
                jobPostingMapper,
                resourceItemMapper,
                userMapper,
                new SearchSyncProperties());

        SearchIndexSyncService.SyncResult result = syncService.refreshUserDocuments(2L);

        assertThat(result.total()).isEqualTo(2);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> docsCaptor = ArgumentCaptor.forClass(List.class);
        verify(bulkIndexer, times(2)).bulkIndex(eq("campus-platform"), docsCaptor.capture());
        List<UnifiedSearchDocument> documents = docsCaptor.getAllValues().stream()
                .flatMap(List::stream)
                .map(UnifiedSearchDocument.class::cast)
                .toList();
        assertThat(documents.stream()
                .map(document -> org.assertj.core.groups.Tuple.tuple(
                        document.getId(),
                        document.getAuthorName()))
                .toList())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("post_10", "FutureRunner"),
                        org.assertj.core.groups.Tuple.tuple("resource_20", "FutureRunner"));
    }

    private ElasticsearchException elasticsearchException(int status, String type, String reason) {
        return new ElasticsearchException("es/indices.create", ErrorResponse.of(response -> response
                .status(status)
                .error(error -> error
                        .type(type)
                        .reason(reason))));
    }
}
