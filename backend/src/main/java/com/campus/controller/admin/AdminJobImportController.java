package com.campus.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campus.common.Result;
import com.campus.dto.AdminJobImportResponse;
import com.campus.service.JobBatchImportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "管理-岗位导入", description = "第三方岗位数据批量导入")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/jobs")
public class AdminJobImportController {

    private final JobBatchImportService jobBatchImportService;

    public AdminJobImportController(JobBatchImportService jobBatchImportService) {
        this.jobBatchImportService = jobBatchImportService;
    }

    @Operation(summary = "批量导入岗位")
    @ApiResponse(responseCode = "200", description = "导入成功")
    @PostMapping("/import")
    public Result<AdminJobImportResponse> importJobs(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        return Result.success(jobBatchImportService.importJobs(authentication.getName(), file));
    }
}
