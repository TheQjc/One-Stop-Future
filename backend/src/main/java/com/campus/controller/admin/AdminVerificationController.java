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

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/verifications")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    public AdminVerificationController(AdminVerificationService adminVerificationService) {
        this.adminVerificationService = adminVerificationService;
    }

    @GetMapping("/dashboard")
    public Result<AdminVerificationDashboardResponse> dashboard() {
        return Result.success(adminVerificationService.getDashboard());
    }

    @GetMapping
    public Result<List<AdminVerificationDashboardResponse.VerificationApplicationSummary>> list() {
        return Result.success(adminVerificationService.listApplications());
    }

    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, Authentication authentication,
            @Validated @RequestBody AdminVerificationReviewRequest request) {
        adminVerificationService.review(id, authentication.getName(), request);
        return Result.success();
    }
}
