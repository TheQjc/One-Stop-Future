package com.campus.dto;

import jakarta.validation.constraints.NotBlank;

public record NoticeCreateRequest(
        @NotBlank String title,
        @NotBlank String content,
        String category,
        Integer isTop) {
}
