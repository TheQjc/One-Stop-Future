package com.campus.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.config.ElasticsearchIntegrationProperties;
import com.campus.service.SearchHealthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "搜索健康", description = "Elasticsearch 健康检查与索引状态")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchHealthController {

    private final SearchHealthService searchHealthService;
    private final ElasticsearchIntegrationProperties esProperties;

    @Operation(summary = "检查 Elasticsearch 健康状态")
    @GetMapping("/health")
    public Result<SearchHealthService.HealthStatus> health() {
        return Result.success(searchHealthService.checkElasticsearchHealth());
    }
}
