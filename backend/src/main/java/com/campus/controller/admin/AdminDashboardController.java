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
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Validated
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public Result<AdminDashboardSummaryResponse> summary() {
        return Result.success(adminDashboardService.getSummary());
    }

    @GetMapping("/charts")
    public Result<AdminDashboardChartsResponse> getCharts(@RequestParam(defaultValue = "30") int days) {
        return Result.success(adminDashboardService.getDashboardCharts(days));
    }

    @GetMapping("/export")
    public void exportDashboardData(HttpServletResponse response, @RequestParam(defaultValue = "30") int days) throws IOException {
        adminDashboardService.exportDashboardData(response, days);
    }
}
