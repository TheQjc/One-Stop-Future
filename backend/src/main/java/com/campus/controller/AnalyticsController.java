package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AnalyticsSummaryResponse;
import com.campus.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "分析", description = "平台统计概览与运营数据")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "获取平台统计概览")
    @GetMapping("/summary")
    public Result<AnalyticsSummaryResponse> summary(@RequestParam(required = false) String period,
            Authentication authentication) {
        String identity = authentication == null ? null : authentication.getName();
        return Result.success(analyticsService.summary(identity, period));
    }
}

