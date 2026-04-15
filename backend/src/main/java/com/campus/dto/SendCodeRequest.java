package com.campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendCodeRequest(
        @NotBlank @Pattern(regexp = "^\\d{11}$", message = "must be 11 digits") String phone,
        @NotBlank String purpose) {
}
