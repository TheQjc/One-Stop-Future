package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.HomeSummaryResponse;
import com.campus.service.HomeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "首页", description = "首页概览卡片数据")
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @Operation(summary = "获取首页概览数据")
    @GetMapping("/summary")
    public Result<HomeSummaryResponse> summary(Authentication authentication) {
        return Result.success(homeService.getSummary(authentication));
    }
}
