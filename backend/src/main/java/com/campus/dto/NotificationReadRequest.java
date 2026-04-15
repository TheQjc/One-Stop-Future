package com.campus.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationReadRequest(@NotNull Long id) {
}
