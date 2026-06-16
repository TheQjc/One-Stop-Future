package com.campus.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AdminJobListResponse;
import com.campus.dto.AdminJobSyncResponse;
import com.campus.dto.CreateJobRequest;
import com.campus.dto.JobDetailResponse;
import com.campus.dto.UpdateJobRequest;
import com.campus.service.AdminJobService;
import com.campus.service.ThirdPartyJobSyncService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理-岗位", description = "岗位管理（CRUD 与状态）")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/jobs")
public class AdminJobController {

    private final AdminJobService adminJobService;
    private final ThirdPartyJobSyncService thirdPartyJobSyncService;

    public AdminJobController(AdminJobService adminJobService, ThirdPartyJobSyncService thirdPartyJobSyncService) {
        this.adminJobService = adminJobService;
        this.thirdPartyJobSyncService = thirdPartyJobSyncService;
    }

    @Operation(summary = "获取岗位列表")
    @GetMapping
    public Result<AdminJobListResponse> list() {
        return Result.success(adminJobService.listJobs());
    }

    @Operation(summary = "创建岗位")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<JobDetailResponse> create(Authentication authentication,
            @Validated @RequestBody CreateJobRequest request) {
        return Result.success(adminJobService.createJob(authentication.getName(), request));
    }

    @Operation(summary = "同步第三方岗位")
    @ApiResponse(responseCode = "200", description = "同步成功")
    @PostMapping("/sync")
    public Result<AdminJobSyncResponse> sync(Authentication authentication) {
        return Result.success(thirdPartyJobSyncService.syncJobs(authentication.getName()));
    }

    @Operation(summary = "更新岗位")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public Result<JobDetailResponse> update(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody UpdateJobRequest request) {
        return Result.success(adminJobService.updateJob(authentication.getName(), id, request));
    }

    @Operation(summary = "发布岗位")
    @ApiResponse(responseCode = "200", description = "发布成功")
    @PostMapping("/{id}/publish")
    public Result<JobDetailResponse> publish(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminJobService.publishJob(authentication.getName(), id));
    }

    @Operation(summary = "下架岗位")
    @ApiResponse(responseCode = "200", description = "下架成功")
    @PostMapping("/{id}/offline")
    public Result<JobDetailResponse> offline(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminJobService.offlineJob(authentication.getName(), id));
    }

    @Operation(summary = "删除岗位")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable Long id, Authentication authentication) {
        adminJobService.deleteJob(authentication.getName(), id);
        return Result.success();
    }
}
