package com.campus.dto;

import java.time.LocalDate;

public record AnalyticsTrendRow(LocalDate bucketDate, int total) {
}

