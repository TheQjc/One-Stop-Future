package com.campus.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.DiscoverResponse;
import com.campus.service.DiscoverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "发现", description = "热门排行与推荐内容")
@Validated
@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final DiscoverService discoverService;

    public DiscoverController(DiscoverService discoverService) {
        this.discoverService = discoverService;
    }

    @Operation(summary = "获取热门排行与推荐")
    @GetMapping
    public Result<DiscoverResponse> discover(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer limit) {
        return Result.success(discoverService.discover(tab, period, limit));
    }
}
