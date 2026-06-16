package com.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "must be 11 digits") String phone,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be 6 digits") String verificationCode,
        @NotBlank @Size(max = 50, message = "nickname must be at most 50 characters") String nickname) {
}
