package com.campus.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionSchoolCompareRequest(
        @NotNull
        @Size(min = 2, max = 4)
        List<@NotNull Long> schoolIds) {
}
