package com.campus.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.campus.common.JobEducationRequirement;
import com.campus.common.JobType;

@Component
public class JobPostingFieldNormalizer {

    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String requiredText(String value, int maxLength, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("value is required");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value exceeds max length");
        }
        return normalized;
    }

    public String optionalText(String value, int maxLength, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("value exceeds max length");
        }
        return normalized;
    }

    public String normalizeJobType(String value) {
        String normalized = requiredText(value, 20, "jobType");
        try {
            return JobType.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid job type");
        }
    }

    public String normalizeEducationRequirement(String value) {
        String normalized = requiredText(value, 20, "educationRequirement");
        try {
            return JobEducationRequirement.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("invalid education requirement");
        }
    }

    public String normalizeSourceUrl(String value) {
        String normalized = requiredText(value, 500, "sourceUrl");
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("invalid source url");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            if ("invalid source url".equals(exception.getMessage())
                    || "value is required".equals(exception.getMessage())
                    || "value exceeds max length".equals(exception.getMessage())) {
                throw exception;
            }
            throw new IllegalArgumentException("invalid source url");
        }
    }

    public LocalDateTime parseDeadline(String value) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized, DEADLINE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("invalid deadline format");
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }
}
