package com.campus.controller.admin;

import com.campus.common.Result;
import com.campus.service.SearchHealthService;
import com.campus.service.SearchIndexSyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理-搜索索引", description = "搜索索引同步触发")
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

    @Operation(summary = "重建搜索索引")
    @ApiResponse(responseCode = "200", description = "重建成功")
    @PostMapping("/reindex")
    public Result<ReindexResponse> reindex() {
        try {
            long start = System.currentTimeMillis();
            SearchIndexSyncService.SyncResult result = syncService.reindexAll();
            return Result.success(new ReindexResponse(
                    "Reindex completed successfully",
                    result.posts(),
                    result.jobs(),
                    result.resources(),
                    result.total(),
                    System.currentTimeMillis() - start));
        } catch (java.io.IOException e) {
            return Result.error(500, "搜索索引重建失败，请稍后重试");
        }
    }

    @Operation(summary = "检查搜索健康状态")
    @GetMapping("/health")
    public Result<SearchHealthService.HealthStatus> health() {
        return Result.success(healthService.checkElasticsearchHealth());
    }
}
