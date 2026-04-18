package com.campus.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.campus.common.CommunityPostStatus;
import com.campus.common.JobPostingStatus;
import com.campus.common.ResourceStatus;
import com.campus.dto.AdminCommunityPostListResponse;
import com.campus.dto.AdminDashboardSummaryResponse;
import com.campus.dto.AdminJobListResponse;
import com.campus.dto.AdminResourceListResponse;
import com.campus.dto.AdminVerificationDashboardResponse;
import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;
import com.campus.entity.User;
import com.campus.entity.VerificationApplication;
import com.campus.mapper.AdminDashboardReadMapper;

@Service
public class AdminDashboardService {

    private static final int RECENT_LIMIT = 5;

    private final AdminDashboardReadMapper adminDashboardReadMapper;
    private final UserService userService;
    private final ResourceService resourceService;

    public AdminDashboardService(AdminDashboardReadMapper adminDashboardReadMapper, UserService userService,
            ResourceService resourceService) {
        this.adminDashboardReadMapper = adminDashboardReadMapper;
        this.userService = userService;
        this.resourceService = resourceService;
    }

    public AdminDashboardSummaryResponse getSummary() {
        return new AdminDashboardSummaryResponse(
                buildVerificationSection(),
                buildCommunitySection(),
                buildJobsSection(),
                buildResourcesSection());
    }

    private AdminDashboardSummaryResponse.VerificationSection buildVerificationSection() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<AdminVerificationDashboardResponse.VerificationApplicationSummary> latestPendingApplications =
                adminDashboardReadMapper.selectLatestPendingApplications(RECENT_LIMIT).stream()
                        .map(this::toVerificationSummary)
                        .toList();
        return new AdminDashboardSummaryResponse.VerificationSection(
                adminDashboardReadMapper.countPendingVerificationApplications(),
                adminDashboardReadMapper.countReviewedVerificationApplicationsBetween(startOfDay, startOfDay.plusDays(1)),
                latestPendingApplications);
    }

    private AdminDashboardSummaryResponse.CommunitySection buildCommunitySection() {
        List<AdminCommunityPostListResponse.PostItem> latestPosts = adminDashboardReadMapper
                .selectLatestCommunityPosts(RECENT_LIMIT).stream()
                .map(this::toCommunityPostItem)
                .toList();
        return new AdminDashboardSummaryResponse.CommunitySection(
                adminDashboardReadMapper.countCommunityPosts(),
                adminDashboardReadMapper.countCommunityPostsByStatus(CommunityPostStatus.PUBLISHED.name()),
                adminDashboardReadMapper.countCommunityPostsByStatus(CommunityPostStatus.HIDDEN.name()),
                adminDashboardReadMapper.countCommunityPostsByStatus(CommunityPostStatus.DELETED.name()),
                latestPosts);
    }

    private AdminDashboardSummaryResponse.JobsSection buildJobsSection() {
        List<AdminJobListResponse.JobItem> latestActionableJobs = adminDashboardReadMapper
                .selectLatestActionableJobs(RECENT_LIMIT).stream()
                .map(this::toJobItem)
                .toList();
        return new AdminDashboardSummaryResponse.JobsSection(
                adminDashboardReadMapper.countNonDeletedJobs(),
                adminDashboardReadMapper.countJobsByStatus(JobPostingStatus.DRAFT.name()),
                adminDashboardReadMapper.countJobsByStatus(JobPostingStatus.PUBLISHED.name()),
                adminDashboardReadMapper.countJobsByStatus(JobPostingStatus.OFFLINE.name()),
                latestActionableJobs);
    }

    private AdminDashboardSummaryResponse.ResourcesSection buildResourcesSection() {
        List<AdminResourceListResponse.ResourceItem> latestPendingResources = adminDashboardReadMapper
                .selectLatestPendingResources(RECENT_LIMIT).stream()
                .map(this::toResourceItem)
                .toList();
        return new AdminDashboardSummaryResponse.ResourcesSection(
                adminDashboardReadMapper.countResources(),
                adminDashboardReadMapper.countResourcesByStatus(ResourceStatus.PENDING.name()),
                adminDashboardReadMapper.countResourcesByStatus(ResourceStatus.PUBLISHED.name()),
                adminDashboardReadMapper.countResourcesByStatus(ResourceStatus.REJECTED.name())
                        + adminDashboardReadMapper.countResourcesByStatus(ResourceStatus.OFFLINE.name()),
                latestPendingResources);
    }

    private AdminVerificationDashboardResponse.VerificationApplicationSummary toVerificationSummary(
            VerificationApplication application) {
        User applicant = userService.findByUserId(application.getUserId());
        return new AdminVerificationDashboardResponse.VerificationApplicationSummary(
                application.getId(),
                application.getUserId(),
                applicant == null ? null : applicant.getNickname(),
                application.getRealName(),
                application.getStudentId(),
                application.getStatus(),
                application.getCreatedAt());
    }

    private AdminCommunityPostListResponse.PostItem toCommunityPostItem(CommunityPost post) {
        User author = userService.findByUserId(post.getAuthorId());
        return new AdminCommunityPostListResponse.PostItem(
                post.getId(),
                post.getTag(),
                post.getTitle(),
                post.getStatus(),
                post.getAuthorId(),
                author == null ? "Unknown" : author.getNickname(),
                safeCount(post.getLikeCount()),
                safeCount(post.getCommentCount()),
                safeCount(post.getFavoriteCount()),
                post.getCreatedAt());
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

    private AdminResourceListResponse.ResourceItem toResourceItem(ResourceItem resource) {
        return new AdminResourceListResponse.ResourceItem(
                resource.getId(),
                resource.getTitle(),
                resource.getCategory(),
                resourceService.findUploaderNickname(resource.getUploaderId()),
                resource.getFileName(),
                resource.getFileSize(),
                resource.getDownloadCount(),
                resource.getStatus(),
                resource.getRejectReason(),
                resource.getCreatedAt(),
                resource.getReviewedAt(),
                resource.getPublishedAt(),
                resourceService.isPreviewAvailableForAdmin(resource),
                resourceService.previewKindForAdmin(resource));
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }
}
