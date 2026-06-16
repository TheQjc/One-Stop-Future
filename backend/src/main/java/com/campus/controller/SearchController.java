package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.SearchResponse;
import com.campus.service.SearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "搜索", description = "统一搜索（ES 主检索 + MySQL 降级 + 关键词高亮）")
@Validated
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "统一搜索")
    @GetMapping
    public Result<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort,
            Authentication authentication) {
        return Result.success(searchService.search(q, type, sort, identityOf(authentication)));
    }

    private String identityOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }
}
