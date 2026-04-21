package com.campus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.campus.common.JobPostingStatus;
import com.campus.dto.AdminJobImportValidationError;
import com.campus.entity.JobPosting;

@Component
public class JobPostingDraftFactory {

    private final JobPostingFieldNormalizer fieldNormalizer;

    public JobPostingDraftFactory(JobPostingFieldNormalizer fieldNormalizer) {
        this.fieldNormalizer = fieldNormalizer;
    }

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
        return collect(rowNumber, column, errors, () -> fieldNormalizer.requiredText(value, maxLength, column));
    }

    private String optionalText(int rowNumber, String column, String value, int maxLength,
            List<AdminJobImportValidationError> errors) {
        return collect(rowNumber, column, errors, () -> fieldNormalizer.optionalText(value, maxLength, column));
    }

    private String normalizeJobType(int rowNumber, String value, List<AdminJobImportValidationError> errors) {
        return collect(rowNumber, "jobType", errors, () -> fieldNormalizer.normalizeJobType(value));
    }

    private String normalizeEducationRequirement(int rowNumber, String value,
            List<AdminJobImportValidationError> errors) {
        return collect(rowNumber, "educationRequirement", errors,
                () -> fieldNormalizer.normalizeEducationRequirement(value));
    }

    private String normalizeSourceUrl(int rowNumber, String value, List<AdminJobImportValidationError> errors) {
        return collect(rowNumber, "sourceUrl", errors, () -> fieldNormalizer.normalizeSourceUrl(value));
    }

    private LocalDateTime parseDeadline(int rowNumber, String value, List<AdminJobImportValidationError> errors) {
        return collect(rowNumber, "deadlineAt", errors, () -> fieldNormalizer.parseDeadline(value));
    }

    private AdminJobImportValidationError error(int rowNumber, String column, String message) {
        return new AdminJobImportValidationError(rowNumber, column, message);
    }

    private <T> T collect(int rowNumber, String column, List<AdminJobImportValidationError> errors,
            Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException exception) {
            errors.add(error(rowNumber, column, exception.getMessage()));
            return null;
        }
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
