package com.campus.dto;

import java.util.List;

public record DecisionSchoolListResponse(
        String track,
        String keyword,
        int total,
        List<SchoolItem> schools) {

    public record SchoolItem(
            Long schoolId,
            String name,
            String track,
            String region,
            String tierLabel) {
    }
}

