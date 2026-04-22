package com.campus.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.common.BusinessException;
import com.campus.common.JobApplicationStatus;
import com.campus.common.JobPostingStatus;
import com.campus.common.ResourcePreviewKind;
import com.campus.dto.ApplyJobRequest;
import com.campus.dto.JobApplicationRecordResponse;
import com.campus.dto.MyJobApplicationListResponse;
import com.campus.entity.JobApplication;
import com.campus.entity.JobPosting;
import com.campus.entity.User;
import com.campus.mapper.JobApplicationMapper;
import com.campus.mapper.JobPostingMapper;
import com.campus.preview.ApplicationSnapshotPreviewService;
import com.campus.storage.ResourceFileStorage;

@Service
public class JobApplicationService {

    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final Logger log = LoggerFactory.getLogger(JobApplicationService.class);

    private final JobApplicationMapper jobApplicationMapper;
    private final JobPostingMapper jobPostingMapper;
    private final UserService userService;
    private final ResourceFileStorage resourceFileStorage;
    private final ApplicationSnapshotPreviewService applicationSnapshotPreviewService;

    public JobApplicationService(JobApplicationMapper jobApplicationMapper, JobPostingMapper jobPostingMapper,
            UserService userService, ResourceFileStorage resourceFileStorage,
            ApplicationSnapshotPreviewService applicationSnapshotPreviewService) {
        this.jobApplicationMapper = jobApplicationMapper;
        this.jobPostingMapper = jobPostingMapper;
        this.userService = userService;
        this.resourceFileStorage = resourceFileStorage;
        this.applicationSnapshotPreviewService = applicationSnapshotPreviewService;
    }

    @Transactional
    public JobApplicationRecordResponse apply(String identity, Long jobId, ApplyJobRequest request) {
        User applicant = userService.requireByIdentity(identity);
        Long resumeId = request == null ? null : request.resumeId();
        if (resumeId == null) {
            throw new BusinessException(400, "resume is required");
        }

        JobPosting job = requirePublishedJob(jobId);
        JobApplicationMapper.ResumeSnapshotSource resume = requireOwnedResume(applicant.getId(), resumeId);

        if (jobApplicationMapper.selectByJobIdAndApplicantUserId(job.getId(), applicant.getId()) != null) {
            throw new BusinessException(400, "already applied to this job");
        }

        String snapshotStorageKey = copyResumeSnapshot(resume);
        LocalDateTime now = LocalDateTime.now();

        JobApplication application = new JobApplication();
        application.setJobId(job.getId());
        application.setApplicantUserId(applicant.getId());
        application.setResumeId(resume.getId());
        application.setStatus(JobApplicationStatus.SUBMITTED.name());
        application.setResumeTitleSnapshot(resume.getTitle());
        application.setResumeFileNameSnapshot(resume.getFileName());
        application.setResumeFileExtSnapshot(resume.getFileExt());
        application.setResumeContentTypeSnapshot(resume.getContentType());
        application.setResumeFileSizeSnapshot(resume.getFileSize());
        application.setResumeStorageKeySnapshot(snapshotStorageKey);
        application.setSubmittedAt(now);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);

        try {
            jobApplicationMapper.insert(application);
        } catch (DuplicateKeyException exception) {
            tryDeleteSnapshotFile(snapshotStorageKey);
            throw new BusinessException(400, "already applied to this job");
        } catch (RuntimeException exception) {
            tryDeleteSnapshotFile(snapshotStorageKey);
            throw exception;
        }

        return toRecord(application);
    }

    public MyJobApplicationListResponse listMine(String identity) {
        User applicant = userService.requireByIdentity(identity);
        List<MyJobApplicationListResponse.ApplicationItem> applications = jobApplicationMapper
                .selectMyApplications(applicant.getId(), DEFAULT_LIST_LIMIT)
                .stream()
                .map(this::toMyApplicationItem)
                .toList();
        return new MyJobApplicationListResponse(
                jobApplicationMapper.countByApplicantUserId(applicant.getId()),
                applications);
    }

    public DownloadedApplicationResume downloadSnapshot(String identity, Long applicationId) {
        User applicant = userService.requireByIdentity(identity);
        JobApplication application = requireOwnedApplication(applicant.getId(), applicationId);
        return openSnapshot(application, "application resume snapshot unavailable");
    }

    public ApplicationSnapshotPreviewService.PreviewFile previewSnapshot(String identity, Long applicationId) {
        User applicant = userService.requireByIdentity(identity);
        JobApplication application = requireOwnedApplication(applicant.getId(), applicationId);
        return applicationSnapshotPreviewService.preview(application,
                () -> openSnapshotInputStream(application, "application resume preview unavailable"));
    }

    private JobPosting requirePublishedJob(Long jobId) {
        JobPosting job = jobPostingMapper.selectById(jobId);
        if (job == null || !JobPostingStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BusinessException(404, "job not found");
        }
        return job;
    }

    private JobApplicationMapper.ResumeSnapshotSource requireOwnedResume(Long userId, Long resumeId) {
        JobApplicationMapper.ResumeSnapshotSource resume = jobApplicationMapper.selectOwnedResume(resumeId, userId);
        if (resume == null) {
            throw new BusinessException(404, "resume not found");
        }
        return resume;
    }

    private JobApplication requireOwnedApplication(Long userId, Long applicationId) {
        JobApplication application = jobApplicationMapper.selectById(applicationId);
        if (application == null || !userId.equals(application.getApplicantUserId())) {
            throw new BusinessException(404, "application not found");
        }
        return application;
    }

    private String copyResumeSnapshot(JobApplicationMapper.ResumeSnapshotSource resume) {
        try (InputStream inputStream = resourceFileStorage.open(resume.getStorageKey())) {
            return resourceFileStorage.store(resume.getFileName(), inputStream);
        } catch (IOException exception) {
            throw new BusinessException(500, "failed to store application resume snapshot");
        }
    }

    private void tryDeleteSnapshotFile(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            if (!resourceFileStorage.exists(storageKey)) {
                return;
            }
            resourceFileStorage.delete(storageKey);
        } catch (IOException exception) {
            log.warn("Failed to delete duplicate-apply snapshot file: {}", storageKey, exception);
        }
    }

    private JobApplicationRecordResponse toRecord(JobApplication application) {
        return new JobApplicationRecordResponse(
                application.getId(),
                application.getJobId(),
                application.getStatus(),
                application.getResumeId(),
                application.getResumeTitleSnapshot(),
                application.getResumeFileNameSnapshot(),
                application.getSubmittedAt());
    }

    private MyJobApplicationListResponse.ApplicationItem toMyApplicationItem(JobApplicationMapper.MyApplicationRow row) {
        ResourcePreviewKind previewKind = applicationSnapshotPreviewService.previewKindOf(
                row.getResumeFileExtSnapshot(),
                row.getResumeContentTypeSnapshot());
        return new MyJobApplicationListResponse.ApplicationItem(
                row.getId(),
                row.getJobId(),
                row.getJobTitle(),
                row.getCompanyName(),
                row.getCity(),
                row.getStatus(),
                row.getResumeTitleSnapshot(),
                row.getResumeFileNameSnapshot(),
                previewKind != ResourcePreviewKind.NONE,
                previewKind,
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
