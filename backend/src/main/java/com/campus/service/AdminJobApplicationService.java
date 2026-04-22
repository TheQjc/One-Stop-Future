package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.common.ResourcePreviewKind;
import com.campus.dto.AdminJobApplicationListResponse;
import com.campus.entity.JobApplication;
import com.campus.mapper.JobApplicationMapper;
import com.campus.preview.ApplicationSnapshotPreviewService;
import com.campus.storage.ResourceFileStorage;

@Service
public class AdminJobApplicationService {

    private static final int ADMIN_LIST_LIMIT = 100;

    private final JobApplicationMapper jobApplicationMapper;
    private final ResourceFileStorage resourceFileStorage;
    private final ApplicationSnapshotPreviewService applicationSnapshotPreviewService;

    public AdminJobApplicationService(JobApplicationMapper jobApplicationMapper,
            ResourceFileStorage resourceFileStorage,
            ApplicationSnapshotPreviewService applicationSnapshotPreviewService) {
        this.jobApplicationMapper = jobApplicationMapper;
        this.resourceFileStorage = resourceFileStorage;
        this.applicationSnapshotPreviewService = applicationSnapshotPreviewService;
    }

    public AdminJobApplicationListResponse listApplications() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        List<AdminJobApplicationListResponse.ApplicationItem> applications = jobApplicationMapper
                .selectAdminApplications(ADMIN_LIST_LIMIT)
                .stream()
                .map(this::toAdminApplicationItem)
                .toList();
        return new AdminJobApplicationListResponse(
                jobApplicationMapper.countAllApplications(),
                jobApplicationMapper.countSubmittedBetween(startOfToday, startOfTomorrow),
                jobApplicationMapper.countUniqueApplicants(),
                jobApplicationMapper.countUniqueJobs(),
                applications);
    }

    public DownloadedApplicationResume downloadResumeSnapshot(Long applicationId) {
        JobApplication application = requireApplication(applicationId);
        return openSnapshot(application, "application resume snapshot unavailable");
    }

    public ApplicationSnapshotPreviewService.PreviewFile previewResumeSnapshot(Long applicationId) {
        JobApplication application = requireApplication(applicationId);
        return applicationSnapshotPreviewService.preview(application,
                () -> openSnapshotInputStream(application, "application resume preview unavailable"));
    }

    private JobApplication requireApplication(Long applicationId) {
        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "application not found");
        }
        return application;
    }

    private AdminJobApplicationListResponse.ApplicationItem toAdminApplicationItem(
            JobApplicationMapper.AdminApplicationRow row) {
        ResourcePreviewKind previewKind = applicationSnapshotPreviewService.previewKindOf(
                row.getResumeFileExtSnapshot(),
                row.getResumeContentTypeSnapshot());
        return new AdminJobApplicationListResponse.ApplicationItem(
                row.getId(),
                row.getJobId(),
                row.getJobTitle(),
                row.getCompanyName(),
                row.getApplicantUserId(),
                row.getApplicantNickname(),
                row.getResumeFileNameSnapshot(),
                previewKind != ResourcePreviewKind.NONE,
                previewKind,
                row.getStatus(),
                row.getSubmittedAt());
    }

    private DownloadedApplicationResume openSnapshot(JobApplication application, String failureMessage) {
        try {
            if (!resourceFileStorage.exists(application.getResumeStorageKeySnapshot())) {
                throw new BusinessException(500, failureMessage);
            }
            return new DownloadedApplicationResume(
                    application.getResumeFileNameSnapshot(),
                    application.getResumeContentTypeSnapshot(),
                    resourceFileStorage.open(application.getResumeStorageKeySnapshot()));
        } catch (IOException exception) {
            throw new BusinessException(500, failureMessage);
        }
    }

    private InputStream openSnapshotInputStream(JobApplication application, String failureMessage) {
        try {
            if (!resourceFileStorage.exists(application.getResumeStorageKeySnapshot())) {
                throw new BusinessException(500, failureMessage);
            }
            return resourceFileStorage.open(application.getResumeStorageKeySnapshot());
        } catch (IOException exception) {
            throw new BusinessException(500, failureMessage);
        }
    }

    public record DownloadedApplicationResume(String fileName, String contentType, InputStream inputStream) {
    }
}
