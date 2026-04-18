package com.campus.dto;

import java.util.List;

public record ResumeListResponse(
        int total,
        List<ResumeRecordResponse> resumes) {
}
