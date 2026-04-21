package com.campus.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.JobPostingStatus;
import com.campus.config.JobSyncProperties;
import com.campus.dto.AdminJobSyncResponse;
import com.campus.entity.JobPosting;
import com.campus.entity.User;
import com.campus.mapper.JobPostingMapper;

@Service
public class ThirdPartyJobSyncService {

    private static final String ISSUE_TYPE_SKIPPED = "SKIPPED";
    private static final String ISSUE_TYPE_INVALID = "INVALID";
    private static final String MESSAGE_JOB_SYNC_UNAVAILABLE = "job sync unavailable";
    private static final String MESSAGE_INVALID_JOB_SYNC_FEED = "invalid job sync feed";

    private final JobPostingMapper jobPostingMapper;
    private final UserService userService;
    private final JobSyncProperties jobSyncProperties;
    private final JobPostingFieldNormalizer fieldNormalizer;
    private final ThirdPartyJobFeedClient feedClient;

    public ThirdPartyJobSyncService(JobPostingMapper jobPostingMapper, UserService userService,
            JobSyncProperties jobSyncProperties, JobPostingFieldNormalizer fieldNormalizer,
            ThirdPartyJobFeedClient feedClient) {
        this.jobPostingMapper = jobPostingMapper;
        this.userService = userService;
        this.jobSyncProperties = jobSyncProperties;
        this.fieldNormalizer = fieldNormalizer;
        this.feedClient = feedClient;
    }

    @Transactional
    public AdminJobSyncResponse syncJobs(String identity) {
        User admin = userService.requireByIdentity(identity);
        String sourceName = requireSourceName();
        List<ThirdPartyJobFeedItem> feedItems = feedClient.fetchJobs();
        validateFeedDuplicates(feedItems);

        Map<String, JobPosting> existingBySourceUrl = loadExistingBySourceUrl(feedItems);
        LocalDateTime now = LocalDateTime.now();
        List<AdminJobSyncResponse.Issue> issues = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (int index = 0; index < feedItems.size(); index++) {
            int itemIndex = index + 1;
            ThirdPartyJobFeedItem item = feedItems.get(index);

            NormalizedJob normalized;
            try {
                normalized = normalize(item, sourceName);
            } catch (IllegalArgumentException exception) {
                issues.add(new AdminJobSyncResponse.Issue(
                        itemIndex,
                        safeSourceUrl(item.sourceUrl()),
                        ISSUE_TYPE_INVALID,
                        exception.getMessage()));
                continue;
            }

            JobPosting existing = existingBySourceUrl.get(normalized.sourceUrl());
            if (existing != null && JobPostingStatus.DELETED.name().equals(existing.getStatus())) {
                issues.add(new AdminJobSyncResponse.Issue(
                        itemIndex,
                        normalized.sourceUrl(),
                        ISSUE_TYPE_SKIPPED,
                        "job is deleted locally"));
                continue;
            }

            if (existing == null) {
                jobPostingMapper.insert(newDraft(normalized, admin.getId(), now));
                createdCount++;
                continue;
            }

            applySyncUpdate(existing, normalized, admin.getId(), now);
            jobPostingMapper.updateById(existing);
            updatedCount++;
        }

        List<AdminJobSyncResponse.Issue> sortedIssues = issues.stream()
                .sorted(Comparator.comparingInt(AdminJobSyncResponse.Issue::itemIndex))
                .toList();

        return new AdminJobSyncResponse(
                sourceName,
                feedItems.size(),
                createdCount,
                updatedCount,
                countIssues(sortedIssues, ISSUE_TYPE_SKIPPED),
                countIssues(sortedIssues, ISSUE_TYPE_INVALID),
                JobPostingStatus.DRAFT.name(),
                sortedIssues);
    }

    private String requireSourceName() {
        try {
            return fieldNormalizer.requiredText(jobSyncProperties.getSourceName(), 50, "sourcePlatform");
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(500, MESSAGE_JOB_SYNC_UNAVAILABLE);
        }
    }

    private void validateFeedDuplicates(List<ThirdPartyJobFeedItem> feedItems) {
        Map<String, Boolean> seen = new HashMap<>();
        for (ThirdPartyJobFeedItem item : feedItems) {
            String sourceUrl = item.sourceUrl();
            if (sourceUrl == null || sourceUrl.isBlank()) {
                continue;
            }
            String normalized = sourceUrl.trim();
            if (seen.putIfAbsent(normalized, Boolean.TRUE) != null) {
                throw new BusinessException(500, MESSAGE_INVALID_JOB_SYNC_FEED);
            }
        }
    }

    private Map<String, JobPosting> loadExistingBySourceUrl(List<ThirdPartyJobFeedItem> feedItems) {
        List<String> sourceUrls = feedItems.stream()
                .map(ThirdPartyJobFeedItem::sourceUrl)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (sourceUrls.isEmpty()) {
            return Map.of();
        }

        Map<String, JobPosting> existingBySourceUrl = new HashMap<>();
        for (JobPosting job : jobPostingMapper.selectList(new LambdaQueryWrapper<JobPosting>()
                .in(JobPosting::getSourceUrl, sourceUrls))) {
            JobPosting previous = existingBySourceUrl.putIfAbsent(job.getSourceUrl(), job);
            if (previous != null) {
                throw new BusinessException(500, MESSAGE_JOB_SYNC_UNAVAILABLE);
            }
        }
        return existingBySourceUrl;
    }

    private NormalizedJob normalize(ThirdPartyJobFeedItem item, String sourceName) {
        return new NormalizedJob(
                fieldNormalizer.requiredText(item.title(), 120, "title"),
                fieldNormalizer.requiredText(item.companyName(), 80, "companyName"),
                fieldNormalizer.requiredText(item.city(), 80, "city"),
                fieldNormalizer.normalizeJobType(item.jobType()),
                fieldNormalizer.normalizeEducationRequirement(item.educationRequirement()),
                sourceName,
                fieldNormalizer.normalizeSourceUrl(item.sourceUrl()),
                fieldNormalizer.requiredText(item.summary(), 300, "summary"),
                fieldNormalizer.optionalText(item.content(), 10000, "content"),
                fieldNormalizer.parseDeadline(item.deadlineAt()));
    }

    private JobPosting newDraft(NormalizedJob normalized, Long adminId, LocalDateTime now) {
        JobPosting job = new JobPosting();
        job.setTitle(normalized.title());
        job.setCompanyName(normalized.companyName());
        job.setCity(normalized.city());
        job.setJobType(normalized.jobType());
        job.setEducationRequirement(normalized.educationRequirement());
        job.setSourcePlatform(normalized.sourcePlatform());
        job.setSourceUrl(normalized.sourceUrl());
        job.setSummary(normalized.summary());
        job.setContent(normalized.content());
        job.setDeadlineAt(normalized.deadlineAt());
        job.setStatus(JobPostingStatus.DRAFT.name());
        job.setPublishedAt(null);
        job.setCreatedBy(adminId);
        job.setUpdatedBy(adminId);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return job;
    }

    private void applySyncUpdate(JobPosting job, NormalizedJob normalized, Long adminId, LocalDateTime now) {
        job.setTitle(normalized.title());
        job.setCompanyName(normalized.companyName());
        job.setCity(normalized.city());
        job.setJobType(normalized.jobType());
        job.setEducationRequirement(normalized.educationRequirement());
        job.setSourcePlatform(normalized.sourcePlatform());
        job.setSourceUrl(normalized.sourceUrl());
        job.setSummary(normalized.summary());
        job.setContent(normalized.content());
        job.setDeadlineAt(normalized.deadlineAt());
        job.setUpdatedBy(adminId);
        job.setUpdatedAt(now);
    }

    private int countIssues(List<AdminJobSyncResponse.Issue> issues, String type) {
        return (int) issues.stream()
                .filter(issue -> type.equals(issue.type()))
                .count();
    }

    private String safeSourceUrl(String sourceUrl) {
        if (sourceUrl == null) {
            return null;
        }
        String trimmed = sourceUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record NormalizedJob(
            String title,
            String companyName,
            String city,
            String jobType,
            String educationRequirement,
            String sourcePlatform,
            String sourceUrl,
            String summary,
            String content,
            LocalDateTime deadlineAt) {
    }
}
