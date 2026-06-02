package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.ApplyJobRequest;
import com.campus.dto.JobDetailResponse;
import com.campus.dto.JobApplicationRecordResponse;
import com.campus.dto.JobListResponse;
import com.campus.service.JobApplicationService;
import com.campus.service.JobService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "岗位", description = "岗位浏览、筛选、申请与收藏")
@Validated
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobApplicationService jobApplicationService;

    public JobController(JobService jobService, JobApplicationService jobApplicationService) {
        this.jobService = jobService;
        this.jobApplicationService = jobApplicationService;
    }

    @Operation(summary = "获取岗位列表")
    @GetMapping
    public Result<JobListResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String educationRequirement,
            @RequestParam(required = false) String sourcePlatform,
            Authentication authentication) {
        return Result.success(jobService.listJobs(keyword, city, jobType, educationRequirement, sourcePlatform,
                identityOf(authentication)));
    }

    @Operation(summary = "获取岗位详情")
    @GetMapping("/{id}")
    public Result<JobDetailResponse> detail(@PathVariable Long id, Authentication authentication) {
        return Result.success(jobService.getJobDetail(id, identityOf(authentication)));
    }

    @Operation(summary = "申请岗位")
    @ApiResponse(responseCode = "200", description = "申请成功")
    @PostMapping("/{id}/apply")
    public Result<JobApplicationRecordResponse> apply(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody ApplyJobRequest request) {
        return Result.success(jobApplicationService.apply(authentication.getName(), id, request));
    }

    @Operation(summary = "收藏岗位")
    @ApiResponse(responseCode = "200", description = "收藏成功")
    @PostMapping("/{id}/favorite")
    public Result<JobDetailResponse> favorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(jobService.favoriteJob(authentication.getName(), id));
    }

    @Operation(summary = "取消收藏岗位")
    @DeleteMapping("/{id}/favorite")
    public Result<JobDetailResponse> unfavorite(@PathVariable Long id, Authentication authentication) {
        return Result.success(jobService.unfavoriteJob(authentication.getName(), id));
    }

    private String identityOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getName();
    }
}
