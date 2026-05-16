package com.campus.controller.admin;

import com.campus.common.Result;
import com.campus.service.SearchHealthService;
import com.campus.service.SearchIndexSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final SearchIndexSyncService syncService;
    private final SearchHealthService healthService;

    public record ReindexResponse(
            String message,
            int postsIndexed,
            int jobsIndexed,
            int resourcesIndexed,
            int total,
            long durationMs) {
    }

    @PostMapping("/reindex")
    public Result<ReindexResponse> reindex() {
        try {
            long start = System.currentTimeMillis();
            SearchIndexSyncService.SyncResult result = syncService.fullReindex();
            return Result.success(new ReindexResponse(
                    "Reindex completed successfully",
                    result.posts(),
                    result.jobs(),
                    result.resources(),
                    result.total(),
                    System.currentTimeMillis() - start));
        } catch (java.io.IOException e) {
            return Result.error(500, "Reindex failed: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public Result<SearchHealthService.HealthStatus> health() {
        return Result.success(healthService.checkElasticsearchHealth());
    }
}
