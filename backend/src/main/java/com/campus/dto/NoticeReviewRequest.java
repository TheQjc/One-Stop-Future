package com.campus.dto;

import jakarta.validation.constraints.NotBlank;

public record NoticeReviewRequest(@NotBlank String status) {
}
