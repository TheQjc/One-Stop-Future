package com.campus.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50) String nickname,
        @Size(max = 50) String realName) {
}
