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

@Validated
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

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
