package com.campus.dto;

import jakarta.validation.constraints.Size;

public record AdminResourceReviewRequest(
        @Size(max = 500, message = "reason must be at most 500 characters") String reason) {
}
