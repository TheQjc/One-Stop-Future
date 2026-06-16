package com.campus.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.campus.common.Result;
import com.campus.dto.DecisionAssessmentQuestionResponse;
import com.campus.dto.DecisionAssessmentResultResponse;
import com.campus.dto.DecisionAssessmentSubmissionRequest;
import com.campus.service.DecisionAssessmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "决策评估", description = "职业决策测评问答")
@Validated
@RestController
@RequestMapping("/api/decision/assessment")
public class DecisionAssessmentController {

    private final DecisionAssessmentService decisionAssessmentService;

    public DecisionAssessmentController(DecisionAssessmentService decisionAssessmentService) {
        this.decisionAssessmentService = decisionAssessmentService;
    }

    @Operation(summary = "获取测评问卷")
    @GetMapping("/questions")
    public Result<DecisionAssessmentQuestionResponse> questions() {
        return Result.success(decisionAssessmentService.listQuestions());
    }

    @Operation(summary = "提交测评答案")
    @ApiResponse(responseCode = "200", description = "提交成功")
    @PostMapping("/submissions")
    public Result<DecisionAssessmentResultResponse> submit(@Valid @RequestBody DecisionAssessmentSubmissionRequest request,
            Authentication authentication) {
        return Result.success(decisionAssessmentService.submit(authentication.getName(), request));
    }

    @Operation(summary = "获取最近测评结果")
    @GetMapping("/latest")
    public Result<DecisionAssessmentResultResponse> latest(Authentication authentication) {
        return Result.success(decisionAssessmentService.latestFor(authentication.getName()));
    }
}
