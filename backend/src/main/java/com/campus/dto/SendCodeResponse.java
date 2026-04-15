package com.campus.dto;

public record SendCodeResponse(String purpose, String debugCode, long expiresInSeconds) {
}
