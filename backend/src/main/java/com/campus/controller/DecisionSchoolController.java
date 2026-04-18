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

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/decision")
public class DecisionSchoolController {

    private final DecisionSchoolService schoolService;

    public DecisionSchoolController(DecisionSchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping("/schools")
    public Result<DecisionSchoolListResponse> list(
            @RequestParam(required = false) String track,
            @RequestParam(required = false) String keyword) {
        return Result.success(schoolService.listSchools(track, keyword));
    }

    @PostMapping("/schools/compare")
    public Result<DecisionSchoolCompareResponse> compare(@Valid @RequestBody DecisionSchoolCompareRequest request) {
        return Result.success(schoolService.compare(request));
    }
}

