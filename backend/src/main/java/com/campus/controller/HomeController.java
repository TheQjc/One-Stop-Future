package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.HomeSummaryResponse;
import com.campus.service.HomeService;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/summary")
    public Result<HomeSummaryResponse> summary(Authentication authentication) {
        return Result.success(homeService.getSummary(authentication));
    }
}
