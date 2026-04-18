package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AnalyticsSummaryResponse;
import com.campus.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public Result<AnalyticsSummaryResponse> summary(@RequestParam(required = false) String period,
            Authentication authentication) {
        String identity = authentication == null ? null : authentication.getName();
        return Result.success(analyticsService.summary(identity, period));
    }
}

