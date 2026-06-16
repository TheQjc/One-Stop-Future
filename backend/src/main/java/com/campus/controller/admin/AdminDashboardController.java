package com.campus.controller.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.AdminDashboardSummaryResponse;
import com.campus.dto.AdminDashboardChartsResponse;
import com.campus.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Tag(name = "管理-看板", description = "运营看板与活跃指标导出")
@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @Operation(summary = "获取运营看板概览")
    @GetMapping("/summary")
    public Result<AdminDashboardSummaryResponse> summary() {
        return Result.success(adminDashboardService.getSummary());
    }

    @Operation(summary = "获取运营图表数据")
    @GetMapping("/charts")
    public Result<AdminDashboardChartsResponse> getCharts(@RequestParam(defaultValue = "30") int days) {
        return Result.success(adminDashboardService.getDashboardCharts(days));
    }

    @Operation(summary = "导出运营数据")
    @GetMapping("/export")
    public void exportDashboardData(HttpServletResponse response, @RequestParam(defaultValue = "30") int days) throws IOException {
        adminDashboardService.exportDashboardData(response, days);
    }
}
