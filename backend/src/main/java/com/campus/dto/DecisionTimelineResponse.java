package com.campus.dto;

import java.time.LocalDate;
import java.util.List;

public record DecisionTimelineResponse(
        String track,
        LocalDate anchorDate,
        boolean assessmentRequired,
        List<TimelineItem> items) {

    public record TimelineItem(
            String phaseCode,
            String phaseLabel,
            String title,
            String summary,
            LocalDate targetDate,
            long remainingDays,
            List<String> actionChecklist,
            String resourceHint) {
    }
}

