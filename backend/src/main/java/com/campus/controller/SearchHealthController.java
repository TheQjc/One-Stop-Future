package com.campus.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.config.ElasticsearchIntegrationProperties;
import com.campus.service.SearchHealthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchHealthController {

    private final SearchHealthService searchHealthService;
    private final ElasticsearchIntegrationProperties esProperties;

    @GetMapping("/health")
    public Result<SearchHealthService.HealthStatus> health() {
        return Result.success(searchHealthService.checkElasticsearchHealth());
    }
}
