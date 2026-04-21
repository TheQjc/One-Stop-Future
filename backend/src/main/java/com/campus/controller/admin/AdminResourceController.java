package com.campus.controller.admin;

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
import com.campus.dto.AdminResourceListResponse;
import com.campus.dto.AdminResourceMigrationRequest;
import com.campus.dto.AdminResourceMigrationResponse;
import com.campus.dto.AdminResourcePreviewMigrationRequest;
import com.campus.dto.AdminResourcePreviewMigrationResponse;
import com.campus.dto.AdminResourceReviewRequest;
import com.campus.dto.ResourceDetailResponse;
import com.campus.service.AdminResourceMigrationService;
import com.campus.service.AdminResourcePreviewMigrationService;
import com.campus.service.AdminResourceService;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/resources")
public class AdminResourceController {

    private final AdminResourceService adminResourceService;
    private final AdminResourceMigrationService adminResourceMigrationService;
    private final AdminResourcePreviewMigrationService adminResourcePreviewMigrationService;

    public AdminResourceController(AdminResourceService adminResourceService,
            AdminResourceMigrationService adminResourceMigrationService,
            AdminResourcePreviewMigrationService adminResourcePreviewMigrationService) {
        this.adminResourceService = adminResourceService;
        this.adminResourceMigrationService = adminResourceMigrationService;
        this.adminResourcePreviewMigrationService = adminResourcePreviewMigrationService;
    }

    @GetMapping
    public Result<AdminResourceListResponse> list() {
        return Result.success(adminResourceService.listResources());
    }

    @PostMapping("/{id}/publish")
    public Result<ResourceDetailResponse> publish(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminResourceService.publishResource(authentication.getName(), id));
    }

    @PostMapping("/{id}/reject")
    public Result<ResourceDetailResponse> reject(@PathVariable Long id, Authentication authentication,
            @RequestBody(required = false) AdminResourceReviewRequest request) {
        String reason = request == null ? null : request.reason();
        return Result.success(adminResourceService.rejectResource(authentication.getName(), id, reason));
    }

    @PostMapping("/{id}/offline")
    public Result<ResourceDetailResponse> offline(@PathVariable Long id, Authentication authentication) {
        return Result.success(adminResourceService.offlineResource(authentication.getName(), id));
    }

    @PostMapping("/migrate-to-minio")
    public Result<AdminResourceMigrationResponse> migrateToMinio(
            Authentication authentication,
            @Validated @RequestBody(required = false) AdminResourceMigrationRequest request) {
        AdminResourceMigrationRequest normalizedRequest = request == null
                ? new AdminResourceMigrationRequest(null, null, null, null, null, null)
                : request;
        return Result.success(
                adminResourceMigrationService.migrateResources(authentication.getName(), normalizedRequest));
    }

    @PostMapping("/migrate-preview-artifacts-to-minio")
    public Result<AdminResourcePreviewMigrationResponse> migratePreviewArtifactsToMinio(
            Authentication authentication,
            @Validated @RequestBody(required = false) AdminResourcePreviewMigrationRequest request) {
        AdminResourcePreviewMigrationRequest normalizedRequest = request == null
                ? new AdminResourcePreviewMigrationRequest(null, null, null, null, null, null)
                : request;
        return Result.success(
                adminResourcePreviewMigrationService.migratePreviewArtifacts(authentication.getName(), normalizedRequest));
    }
}
