package com.campus.controller.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.campus.common.Result;
import com.campus.service.SearchHealthService;
import com.campus.service.SearchIndexSyncService;

class AdminSearchControllerTests {

    @Test
    void reindexUsesDeleteAndRecreatePath() {
        RecordingSearchIndexSyncService syncService = new RecordingSearchIndexSyncService();
        AdminSearchController controller = new AdminSearchController(syncService, null);

        Result<AdminSearchController.ReindexResponse> response = controller.reindex();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.data().total()).isEqualTo(6);
        assertThat(syncService.reindexAllCalled).isTrue();
        assertThat(syncService.fullReindexCalledDirectly).isFalse();
    }

    @Test
    void reindexFailureReturnsChineseMessage() {
        RecordingSearchIndexSyncService syncService = new RecordingSearchIndexSyncService();
        syncService.failReindex = true;
        AdminSearchController controller = new AdminSearchController(syncService, null);

        Result<AdminSearchController.ReindexResponse> response = controller.reindex();

        assertThat(response.code()).isEqualTo(500);
        assertThat(response.message()).isEqualTo("搜索索引重建失败，请稍后重试");
    }

    private static class RecordingSearchIndexSyncService extends SearchIndexSyncService {

        private boolean reindexAllCalled;
        private boolean fullReindexCalledDirectly;
        private boolean failReindex;

        RecordingSearchIndexSyncService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public SyncResult fullReindex() {
            fullReindexCalledDirectly = true;
            return new SyncResult(1, 2, 3, 6, 10);
        }

        @Override
        public SyncResult reindexAll() throws IOException {
            reindexAllCalled = true;
            if (failReindex) {
                throw new IOException("Elasticsearch timeout");
            }
            return new SyncResult(1, 2, 3, 6, 10);
        }
    }
}
