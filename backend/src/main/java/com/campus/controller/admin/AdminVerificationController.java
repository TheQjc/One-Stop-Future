package com.campus.controller.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AdminVerificationDashboardResponse;
import com.campus.dto.AdminVerificationReviewRequest;
import com.campus.service.AdminVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理-认证审核", description = "学生认证申请审批")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/verifications")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    public AdminVerificationController(AdminVerificationService adminVerificationService) {
        this.adminVerificationService = adminVerificationService;
    }

    @Operation(summary = "获取认证审核看板")
    @GetMapping("/dashboard")
    public Result<AdminVerificationDashboardResponse> dashboard() {
        return Result.success(adminVerificationService.getDashboard());
    }

    @Operation(summary = "获取认证申请列表")
    @GetMapping
    public Result<List<AdminVerificationDashboardResponse.VerificationApplicationSummary>> list() {
        return Result.success(adminVerificationService.listApplications());
    }

    @Operation(summary = "审核认证申请")
    @ApiResponse(responseCode = "200", description = "审核完成")
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody AdminVerificationReviewRequest request) {
        adminVerificationService.review(id, authentication.getName(), request);
        return Result.success();
    }
}
