package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.campus.common.BusinessException;
import com.campus.dto.AdminJobApplicationListResponse;
import com.campus.entity.JobApplication;
import com.campus.mapper.JobApplicationMapper;
import com.campus.storage.ResourceFileStorage;

@Service
public class AdminJobApplicationService {

    private static final int ADMIN_LIST_LIMIT = 100;

    private final JobApplicationMapper jobApplicationMapper;
    private final ResourceFileStorage resourceFileStorage;

    public AdminJobApplicationService(JobApplicationMapper jobApplicationMapper,
            ResourceFileStorage resourceFileStorage) {
        this.jobApplicationMapper = jobApplicationMapper;
        this.resourceFileStorage = resourceFileStorage;
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
        try {
            if (!resourceFileStorage.exists(application.getResumeStorageKeySnapshot())) {
                throw new BusinessException(500, "application resume snapshot unavailable");
            }
            InputStream inputStream = resourceFileStorage.open(application.getResumeStorageKeySnapshot());
            return new DownloadedApplicationResume(
                    application.getResumeFileNameSnapshot(),
                    application.getResumeContentTypeSnapshot(),
                    inputStream);
        } catch (IOException exception) {
            throw new BusinessException(500, "application resume snapshot unavailable");
        }
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
        return new AdminJobApplicationListResponse.ApplicationItem(
                row.getId(),
                row.getJobId(),
                row.getJobTitle(),
                row.getCompanyName(),
                row.getApplicantUserId(),
                row.getApplicantNickname(),
                row.getResumeFileNameSnapshot(),
                row.getStatus(),
                row.getSubmittedAt());
    }

    public record DownloadedApplicationResume(String fileName, String contentType, InputStream inputStream) {
    }
}
