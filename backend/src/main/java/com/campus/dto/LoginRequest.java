package com.campus.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "must be 11 digits") String phone,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be 6 digits") String verificationCode) {
}
