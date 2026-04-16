package com.campus.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.JobEducationRequirement;
import com.campus.common.JobPostingStatus;
import com.campus.common.JobType;
import com.campus.dto.AdminJobListResponse;
import com.campus.dto.CreateJobRequest;
import com.campus.dto.JobDetailResponse;
import com.campus.dto.UpdateJobRequest;
import com.campus.entity.JobPosting;
import com.campus.entity.User;
import com.campus.mapper.JobPostingMapper;

@Service
public class AdminJobService {

    private static final int ADMIN_LIST_LIMIT = 100;

    private final JobPostingMapper jobPostingMapper;
    private final UserService userService;
    private final JobService jobService;

    public AdminJobService(JobPostingMapper jobPostingMapper, UserService userService, JobService jobService) {
        this.jobPostingMapper = jobPostingMapper;
        this.userService = userService;
        this.jobService = jobService;
    }

    public AdminJobListResponse listJobs() {
        List<AdminJobListResponse.JobItem> jobs = jobPostingMapper.selectList(new LambdaQueryWrapper<JobPosting>()
                        .orderByDesc(JobPosting::getCreatedAt)
                        .orderByDesc(JobPosting::getId)
                        .last("LIMIT " + ADMIN_LIST_LIMIT))
                .stream()
                .map(this::toJobItem)
                .toList();
        return new AdminJobListResponse(jobs.size(), jobs);
    }

    @Transactional
    public JobDetailResponse createJob(String identity, CreateJobRequest request) {
        User admin = userService.requireByIdentity(identity);
        JobPosting job = new JobPosting();
        applyRequest(job, request.title(), request.companyName(), request.city(), request.jobType(),
                request.educationRequirement(), request.sourcePlatform(), request.sourceUrl(), request.summary(),
                request.content(), request.deadlineAt());
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(JobPostingStatus.DRAFT.name());
        job.setPublishedAt(null);
        job.setCreatedBy(admin.getId());
        job.setUpdatedBy(admin.getId());
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobPostingMapper.insert(job);
        return jobService.getJobDetail(job.getId(), identity);
    }

    @Transactional
    public JobDetailResponse updateJob(String identity, Long jobId, UpdateJobRequest request) {
        User admin = userService.requireByIdentity(identity);
        JobPosting job = requireJob(jobId);
        if (JobPostingStatus.DELETED.name().equals(job.getStatus())) {
            throw new BusinessException(400, "deleted job cannot be updated");
        }
        applyRequest(job, request.title(), request.companyName(), request.city(), request.jobType(),
                request.educationRequirement(), request.sourcePlatform(), request.sourceUrl(), request.summary(),
                request.content(), request.deadlineAt());
        job.setUpdatedBy(admin.getId());
        job.setUpdatedAt(LocalDateTime.now());
        jobPostingMapper.updateById(job);
        return jobService.getJobDetail(job.getId(), identity);
    }

    @Transactional
    public JobDetailResponse publishJob(String identity, Long jobId) {
        User admin = userService.requireByIdentity(identity);
        JobPosting job = requireJob(jobId);
        if (JobPostingStatus.DELETED.name().equals(job.getStatus())) {
            throw new BusinessException(400, "deleted job cannot be published");
        }
        validatePublishable(job);
        LocalDateTime now = LocalDateTime.now();
        job.setStatus(JobPostingStatus.PUBLISHED.name());
        job.setPublishedAt(now);
        job.setUpdatedBy(admin.getId());
        job.setUpdatedAt(now);
        jobPostingMapper.updateById(job);
        return jobService.getJobDetail(job.getId(), identity);
    }

    @Transactional
    public JobDetailResponse offlineJob(String identity, Long jobId) {
        User admin = userService.requireByIdentity(identity);
        JobPosting job = requireJob(jobId);
        if (JobPostingStatus.DELETED.name().equals(job.getStatus())) {
            throw new BusinessException(400, "deleted job cannot be offlined");
        }
        if (JobPostingStatus.OFFLINE.name().equals(job.getStatus())) {
            return jobService.getJobDetail(job.getId(), identity);
        }
        if (!JobPostingStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BusinessException(400, "only published job can be offlined");
        }
        job.setStatus(JobPostingStatus.OFFLINE.name());
        job.setUpdatedBy(admin.getId());
        job.setUpdatedAt(LocalDateTime.now());
        jobPostingMapper.updateById(job);
        return jobService.getJobDetail(job.getId(), identity);
    }

    @Transactional
    public void deleteJob(String identity, Long jobId) {
        User admin = userService.requireByIdentity(identity);
        JobPosting job = requireJob(jobId);
        if (JobPostingStatus.DELETED.name().equals(job.getStatus())) {
            return;
        }
        job.setStatus(JobPostingStatus.DELETED.name());
        job.setUpdatedBy(admin.getId());
        job.setUpdatedAt(LocalDateTime.now());
        jobPostingMapper.updateById(job);
    }

    private void applyRequest(JobPosting job, String title, String companyName, String city, String jobType,
            String educationRequirement, String sourcePlatform, String sourceUrl, String summary, String content,
            LocalDateTime deadlineAt) {
        job.setTitle(title.trim());
        job.setCompanyName(companyName.trim());
        job.setCity(city.trim());
        job.setJobType(normalizeJobType(jobType));
        job.setEducationRequirement(normalizeEducationRequirement(educationRequirement));
        job.setSourcePlatform(sourcePlatform.trim());
        job.setSourceUrl(sourceUrl.trim());
        job.setSummary(summary.trim());
        job.setContent(content == null ? null : content.trim());
        job.setDeadlineAt(deadlineAt);
    }

    private void validatePublishable(JobPosting job) {
        if (isBlank(job.getTitle()) || isBlank(job.getCompanyName()) || isBlank(job.getCity()) || isBlank(job.getJobType())
                || isBlank(job.getEducationRequirement()) || isBlank(job.getSourcePlatform()) || isBlank(job.getSourceUrl())
                || isBlank(job.getSummary())) {
            throw new BusinessException(400, "job is not ready for publish");
        }
        validateSourceUrl(job.getSourceUrl());
    }

    private String normalizeJobType(String jobType) {
        try {
            return JobType.valueOf(jobType.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid job type");
        }
    }

    private String normalizeEducationRequirement(String educationRequirement) {
        try {
            return JobEducationRequirement.valueOf(
                    educationRequirement.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid education requirement");
        }
    }

    private void validateSourceUrl(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new BusinessException(400, "invalid source url");
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid source url");
        }
    }

    private JobPosting requireJob(Long jobId) {
        JobPosting job = jobPostingMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "job not found");
        }
        return job;
    }

    private AdminJobListResponse.JobItem toJobItem(JobPosting job) {
        return new AdminJobListResponse.JobItem(
                job.getId(),
                job.getTitle(),
                job.getCompanyName(),
                job.getCity(),
                job.getJobType(),
                job.getEducationRequirement(),
                job.getSourcePlatform(),
                job.getStatus(),
                job.getSummary(),
                job.getDeadlineAt(),
                job.getPublishedAt(),
                job.getUpdatedAt());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
