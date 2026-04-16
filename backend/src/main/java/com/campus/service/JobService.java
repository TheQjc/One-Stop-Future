package com.campus.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.BusinessException;
import com.campus.common.FavoriteTargetType;
import com.campus.common.JobEducationRequirement;
import com.campus.common.JobPostingStatus;
import com.campus.common.JobType;
import com.campus.dto.JobDetailResponse;
import com.campus.dto.JobListResponse;
import com.campus.dto.SearchResponse;
import com.campus.entity.JobPosting;
import com.campus.entity.User;
import com.campus.entity.UserFavorite;
import com.campus.mapper.JobPostingMapper;
import com.campus.mapper.UserFavoriteMapper;

@Service
public class JobService {

    private static final int DEFAULT_LIST_LIMIT = 50;

    private final JobPostingMapper jobPostingMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserService userService;

    public JobService(JobPostingMapper jobPostingMapper, UserFavoriteMapper userFavoriteMapper,
            UserService userService) {
        this.jobPostingMapper = jobPostingMapper;
        this.userFavoriteMapper = userFavoriteMapper;
        this.userService = userService;
    }

    public JobListResponse listJobs(String keyword, String city, String jobType, String educationRequirement,
            String sourcePlatform, String identity) {
        User viewer = findViewer(identity);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedCity = normalizeOptional(city);
        String normalizedJobType = normalizeJobType(jobType);
        String normalizedEducationRequirement = normalizeEducationRequirement(educationRequirement);
        String normalizedSourcePlatform = normalizeOptional(sourcePlatform);

        LambdaQueryWrapper<JobPosting> wrapper = new LambdaQueryWrapper<JobPosting>()
                .eq(JobPosting::getStatus, JobPostingStatus.PUBLISHED.name())
                .orderByDesc(JobPosting::getPublishedAt)
                .orderByDesc(JobPosting::getId)
                .last("LIMIT " + DEFAULT_LIST_LIMIT);

        if (normalizedKeyword != null) {
            wrapper.and(query -> query.like(JobPosting::getTitle, normalizedKeyword)
                    .or()
                    .like(JobPosting::getCompanyName, normalizedKeyword)
                    .or()
                    .like(JobPosting::getSummary, normalizedKeyword));
        }
        if (normalizedCity != null) {
            wrapper.eq(JobPosting::getCity, normalizedCity);
        }
        if (normalizedJobType != null) {
            wrapper.eq(JobPosting::getJobType, normalizedJobType);
        }
        if (normalizedEducationRequirement != null) {
            wrapper.eq(JobPosting::getEducationRequirement, normalizedEducationRequirement);
        }
        if (normalizedSourcePlatform != null) {
            wrapper.eq(JobPosting::getSourcePlatform, normalizedSourcePlatform);
        }

        List<JobListResponse.JobSummary> jobs = jobPostingMapper.selectList(wrapper).stream()
                .map(job -> toJobSummary(job, viewer))
                .toList();

        return new JobListResponse(
                normalizedKeyword,
                normalizedCity,
                normalizedJobType,
                normalizedEducationRequirement,
                normalizedSourcePlatform,
                jobs.size(),
                jobs);
    }

    public JobDetailResponse getJobDetail(Long jobId, String identity) {
        User viewer = findViewer(identity);
        return toJobDetail(requireVisibleJob(jobId, viewer), viewer);
    }

    public List<SearchResponse.SearchResultItem> searchPublishedJobs(String keyword) {
        String normalizedKeyword = normalizeOptional(keyword);
        if (normalizedKeyword == null) {
            return List.of();
        }

        return jobPostingMapper.selectList(new LambdaQueryWrapper<JobPosting>()
                .eq(JobPosting::getStatus, JobPostingStatus.PUBLISHED.name())
                .orderByDesc(JobPosting::getPublishedAt)
                .orderByDesc(JobPosting::getId))
                .stream()
                .filter(job -> containsKeyword(job.getTitle(), normalizedKeyword)
                        || containsKeyword(job.getCompanyName(), normalizedKeyword)
                        || containsKeyword(job.getSummary(), normalizedKeyword)
                        || containsKeyword(job.getContent(), normalizedKeyword))
                .map(job -> new SearchResponse.SearchResultItem(
                        job.getId(),
                        "JOB",
                        job.getTitle(),
                        job.getSummary(),
                        job.getCompanyName(),
                        jobMetaSecondaryOf(job),
                        "/jobs/" + job.getId(),
                        job.getPublishedAt()))
                .toList();
    }

    public JobListResponse listMyJobFavorites(String identity) {
        User viewer = userService.requireByIdentity(identity);
        List<JobListResponse.JobSummary> jobs = userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, viewer.getId())
                        .eq(UserFavorite::getTargetType, FavoriteTargetType.JOB.name())
                        .orderByDesc(UserFavorite::getCreatedAt)
                        .orderByDesc(UserFavorite::getId)
                        .last("LIMIT " + DEFAULT_LIST_LIMIT))
                .stream()
                .map(favorite -> jobPostingMapper.selectById(favorite.getTargetId()))
                .filter(job -> job != null && JobPostingStatus.PUBLISHED.name().equals(job.getStatus()))
                .map(job -> toJobSummary(job, viewer))
                .toList();
        return new JobListResponse(null, null, null, null, null, jobs.size(), jobs);
    }

    @Transactional
    public JobDetailResponse favoriteJob(String identity, Long jobId) {
        User viewer = userService.requireByIdentity(identity);
        JobPosting job = requirePublishedJob(jobId);
        if (!hasFavorite(job.getId(), viewer.getId())) {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(viewer.getId());
            favorite.setTargetType(FavoriteTargetType.JOB.name());
            favorite.setTargetId(job.getId());
            favorite.setCreatedAt(LocalDateTime.now());
            userFavoriteMapper.insert(favorite);
        }
        return toJobDetail(requirePublishedJob(jobId), viewer);
    }

    @Transactional
    public JobDetailResponse unfavoriteJob(String identity, Long jobId) {
        User viewer = userService.requireByIdentity(identity);
        JobPosting job = requirePublishedJob(jobId);
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, viewer.getId())
                .eq(UserFavorite::getTargetType, FavoriteTargetType.JOB.name())
                .eq(UserFavorite::getTargetId, job.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
        }
        return toJobDetail(requirePublishedJob(jobId), viewer);
    }

    private JobPosting requireVisibleJob(Long jobId, User viewer) {
        JobPosting job = jobPostingMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException(404, "job not found");
        }
        if (viewer != null && "ADMIN".equals(viewer.getRole())) {
            return job;
        }
        if (!JobPostingStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BusinessException(404, "job not found");
        }
        return job;
    }

    private JobPosting requirePublishedJob(Long jobId) {
        JobPosting job = jobPostingMapper.selectById(jobId);
        if (job == null || !JobPostingStatus.PUBLISHED.name().equals(job.getStatus())) {
            throw new BusinessException(404, "job not found");
        }
        return job;
    }

    private JobListResponse.JobSummary toJobSummary(JobPosting job, User viewer) {
        return new JobListResponse.JobSummary(
                job.getId(),
                job.getTitle(),
                job.getCompanyName(),
                job.getCity(),
                job.getJobType(),
                job.getEducationRequirement(),
                job.getSourcePlatform(),
                job.getSummary(),
                job.getStatus(),
                job.getDeadlineAt(),
                job.getPublishedAt(),
                viewer != null && hasFavorite(job.getId(), viewer.getId()));
    }

    private JobDetailResponse toJobDetail(JobPosting job, User viewer) {
        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getCompanyName(),
                job.getCity(),
                job.getJobType(),
                job.getEducationRequirement(),
                job.getSourcePlatform(),
                job.getSourceUrl(),
                job.getSummary(),
                job.getContent(),
                job.getStatus(),
                job.getDeadlineAt(),
                job.getPublishedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                viewer != null && hasFavorite(job.getId(), viewer.getId()));
    }

    private boolean hasFavorite(Long jobId, Long userId) {
        return userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, FavoriteTargetType.JOB.name())
                .eq(UserFavorite::getTargetId, jobId)) > 0;
    }

    private User findViewer(String identity) {
        if (identity == null || identity.isBlank() || "anonymousUser".equals(identity)) {
            return null;
        }
        return userService.requireByIdentity(identity);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeJobType(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            return null;
        }
        try {
            return JobType.valueOf(jobType.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid job type");
        }
    }

    private String normalizeEducationRequirement(String educationRequirement) {
        if (educationRequirement == null || educationRequirement.isBlank()) {
            return null;
        }
        try {
            return JobEducationRequirement.valueOf(
                    educationRequirement.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "invalid education requirement");
        }
    }

    private String jobMetaSecondaryOf(JobPosting job) {
        String city = job.getCity() == null ? "" : job.getCity();
        String jobType = job.getJobType() == null ? "" : job.getJobType();
        return city + " / " + jobType;
    }

    private boolean containsKeyword(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}
