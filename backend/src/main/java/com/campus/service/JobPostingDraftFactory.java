package com.campus.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.campus.common.JobEducationRequirement;
import com.campus.common.JobPostingStatus;
import com.campus.common.JobType;
import com.campus.dto.AdminJobImportValidationError;
import com.campus.entity.JobPosting;

@Component
public class JobPostingDraftFactory {

    private static final DateTimeFormatter DEADLINE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BuildResult build(JobImportRow row, Long adminId, LocalDateTime now) {
        List<AdminJobImportValidationError> errors = new ArrayList<>();

        String title = requiredText(row.rowNumber(), "title", row.title(), 120, errors);
        String companyName = requiredText(row.rowNumber(), "companyName", row.companyName(), 80, errors);
        String city = requiredText(row.rowNumber(), "city", row.city(), 80, errors);
        String jobType = normalizeJobType(row.rowNumber(), row.jobType(), errors);
        String educationRequirement = normalizeEducationRequirement(row.rowNumber(), row.educationRequirement(), errors);
        String sourcePlatform = requiredText(row.rowNumber(), "sourcePlatform", row.sourcePlatform(), 50, errors);
        String sourceUrl = normalizeSourceUrl(row.rowNumber(), row.sourceUrl(), errors);
        String summary = requiredText(row.rowNumber(), "summary", row.summary(), 300, errors);
        String content = optionalText(row.rowNumber(), "content", row.content(), 10000, errors);
        LocalDateTime deadlineAt = parseDeadline(row.rowNumber(), row.deadlineAt(), errors);

        if (!errors.isEmpty()) {
            return BuildResult.invalid(errors);
        }

        JobPosting job = new JobPosting();
        job.setTitle(title);
        job.setCompanyName(companyName);
        job.setCity(city);
        job.setJobType(jobType);
        job.setEducationRequirement(educationRequirement);
        job.setSourcePlatform(sourcePlatform);
        job.setSourceUrl(sourceUrl);
        job.setSummary(summary);
        job.setContent(content);
        job.setDeadlineAt(deadlineAt);
        job.setStatus(JobPostingStatus.DRAFT.name());
        job.setPublishedAt(null);
        job.setCreatedBy(adminId);
        job.setUpdatedBy(adminId);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return BuildResult.valid(job);
    }

    private String requiredText(int rowNumber, String column, String value, int maxLength,
            List<AdminJobImportValidationError> errors) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            errors.add(error(rowNumber, column, "value is required"));
            return null;
        }
        if (normalized.length() > maxLength) {
            errors.add(error(rowNumber, column, "value exceeds max length"));
            return null;
        }
        return normalized;
    }

    private String optionalText(int rowNumber, String column, String value, int maxLength,
            List<AdminJobImportValidationError> errors) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            errors.add(error(rowNumber, column, "value exceeds max length"));
            return null;
        }
        return normalized;
    }

    private String normalizeJobType(int rowNumber, String value, List<AdminJobImportValidationError> errors) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            errors.add(error(rowNumber, "jobType", "value is required"));
            return null;
        }
        try {
            return JobType.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            errors.add(error(rowNumber, "jobType", "invalid job type"));
            return null;
        }
    }

    private String normalizeEducationRequirement(int rowNumber, String value,
            List<AdminJobImportValidationError> errors) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            errors.add(error(rowNumber, "educationRequirement", "value is required"));
            return null;
        }
        try {
            return JobEducationRequirement.valueOf(normalized.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            errors.add(error(rowNumber, "educationRequirement", "invalid education requirement"));
            return null;
        }
    }

    private String normalizeSourceUrl(int rowNumber, String value, List<AdminJobImportValidationError> errors) {
        String normalized = requiredText(rowNumber, "sourceUrl", value, 500, errors);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                errors.add(error(rowNumber, "sourceUrl", "invalid source url"));
                return null;
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            errors.add(error(rowNumber, "sourceUrl", "invalid source url"));
            return null;
        }
    }

    private LocalDateTime parseDeadline(int rowNumber, String value, List<AdminJobImportValidationError> errors) {
        String normalized = normalizeText(value);
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized, DEADLINE_FORMATTER);
        } catch (DateTimeParseException exception) {
            errors.add(error(rowNumber, "deadlineAt", "invalid deadline format"));
            return null;
        }
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private AdminJobImportValidationError error(int rowNumber, String column, String message) {
        return new AdminJobImportValidationError(rowNumber, column, message);
    }

    public record BuildResult(JobPosting job, List<AdminJobImportValidationError> errors) {

        public static BuildResult valid(JobPosting job) {
            return new BuildResult(job, List.of());
        }

        public static BuildResult invalid(List<AdminJobImportValidationError> errors) {
            return new BuildResult(null, List.copyOf(errors));
        }

        public boolean isValid() {
            return job != null && errors.isEmpty();
        }
    }
}
