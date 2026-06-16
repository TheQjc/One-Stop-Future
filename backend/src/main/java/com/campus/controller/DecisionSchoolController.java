package com.campus.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.DecisionSchoolCompareRequest;
import com.campus.dto.DecisionSchoolCompareResponse;
import com.campus.dto.DecisionSchoolListResponse;
import com.campus.service.DecisionSchoolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "院校对比", description = "院校数据查询与多校对比")
@Validated
@RestController
@RequestMapping("/api/decision")
public class DecisionSchoolController {

    private final DecisionSchoolService schoolService;

    public DecisionSchoolController(DecisionSchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @Operation(summary = "获取院校列表")
    @GetMapping("/schools")
    public Result<DecisionSchoolListResponse> list(
            @RequestParam(required = false) String track,
            @RequestParam(required = false) String keyword) {
        return Result.success(schoolService.listSchools(track, keyword));
    }

    @Operation(summary = "对比院校")
    @ApiResponse(responseCode = "200", description = "对比成功")
    @PostMapping("/schools/compare")
    public Result<DecisionSchoolCompareResponse> compare(@Valid @RequestBody DecisionSchoolCompareRequest request) {
        return Result.success(schoolService.compare(request));
    }
}

