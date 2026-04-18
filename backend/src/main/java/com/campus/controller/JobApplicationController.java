package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.MyJobApplicationListResponse;
import com.campus.service.JobApplicationService;

@Validated
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping("/mine")
    public Result<MyJobApplicationListResponse> mine(Authentication authentication) {
        return Result.success(jobApplicationService.listMine(authentication.getName()));
    }
}
