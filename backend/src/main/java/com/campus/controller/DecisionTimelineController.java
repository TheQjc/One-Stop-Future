package com.campus.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campus.common.BusinessException;
import com.campus.common.Result;
import com.campus.dto.DecisionTimelineResponse;
import com.campus.service.DecisionTimelineService;

@Validated
@RestController
@RequestMapping("/api/decision")
public class DecisionTimelineController {

    private final DecisionTimelineService timelineService;

    public DecisionTimelineController(DecisionTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/timeline")
    public Result<DecisionTimelineResponse> timeline(
            @RequestParam(required = false) String track,
            @RequestParam(required = false) String anchorDate,
            Authentication authentication) {
        return Result.success(timelineService.timelineFor(authentication.getName(), track, parseAnchorDate(anchorDate)));
    }

    private LocalDate parseAnchorDate(String anchorDate) {
        if (anchorDate == null || anchorDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(anchorDate.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException(400, "invalid anchorDate");
        }
    }
}

