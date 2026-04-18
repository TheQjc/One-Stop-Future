package com.campus.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.Result;
import com.campus.dto.DecisionAssessmentQuestionResponse;
import com.campus.service.DecisionAssessmentService;

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
}

