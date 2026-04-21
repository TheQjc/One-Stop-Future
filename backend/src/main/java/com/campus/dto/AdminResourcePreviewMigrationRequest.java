package com.campus.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminResourcePreviewMigrationRequest(
        Boolean dryRun,
        List<String> statuses,
        List<@NotNull @Positive Long> resourceIds,
        String keyword,
        Boolean onlyMissingInMinio,
        @Min(1) @Max(200) Integer limit) {
}
