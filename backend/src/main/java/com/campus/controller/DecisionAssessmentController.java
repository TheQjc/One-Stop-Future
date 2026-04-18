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

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/decision/assessment")
public class DecisionAssessmentController {

    private final DecisionAssessmentService decisionAssessmentService;

    public DecisionAssessmentController(DecisionAssessmentService decisionAssessmentService) {
        this.decisionAssessmentService = decisionAssessmentService;
    }

    @GetMapping("/questions")
    public Result<DecisionAssessmentQuestionResponse> questions() {
        return Result.success(decisionAssessmentService.listQuestions());
    }

    @PostMapping("/submissions")
    public Result<DecisionAssessmentResultResponse> submit(@Valid @RequestBody DecisionAssessmentSubmissionRequest request,
            Authentication authentication) {
        return Result.success(decisionAssessmentService.submit(authentication.getName(), request));
    }

    @GetMapping("/latest")
    public Result<DecisionAssessmentResultResponse> latest(Authentication authentication) {
        return Result.success(decisionAssessmentService.latestFor(authentication.getName()));
    }
}
