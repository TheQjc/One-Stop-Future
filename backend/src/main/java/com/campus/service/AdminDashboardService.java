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
    private final com.campus.mapper.UserMapper userMapper;
    private final com.campus.mapper.CommunityPostMapper communityPostMapper;
    private final com.campus.mapper.ResourceItemMapper resourceItemMapper;

    public AdminDashboardService(AdminDashboardReadMapper adminDashboardReadMapper, UserService userService,
            ResourceService resourceService, com.campus.mapper.UserMapper userMapper,
            com.campus.mapper.CommunityPostMapper communityPostMapper,
            com.campus.mapper.ResourceItemMapper resourceItemMapper) {
        this.adminDashboardReadMapper = adminDashboardReadMapper;
        this.userService = userService;
        this.resourceService = resourceService;
        this.userMapper = userMapper;
        this.communityPostMapper = communityPostMapper;
        this.resourceItemMapper = resourceItemMapper;
    }

    public AdminDashboardSummaryResponse getSummary() {
        return new AdminDashboardSummaryResponse(
                buildVerificationSection(),
                buildCommunitySection(),
                buildJobsSection(),
                buildResourcesSection());
    }

    public com.campus.dto.AdminDashboardChartsResponse getDashboardCharts(int days) {
        LocalDateTime start = LocalDate.now().minusDays(Math.max(1, days) - 1L).atStartOfDay();
        com.campus.dto.AdminDashboardChartsResponse response = new com.campus.dto.AdminDashboardChartsResponse();
        response.setRegistrationTrends(userMapper.selectRegistrationTrends(start));
        response.setPostTrends(communityPostMapper.selectPostTrends(start));
        response.setActiveUserTrends(communityPostMapper.selectActiveUserTrends(start));
        response.setTagProportions(communityPostMapper.selectTagProportions());
        response.setDownloadRankings(resourceItemMapper.selectDownloadRankings());
        return response;
    }

    public void exportDashboardData(jakarta.servlet.http.HttpServletResponse response, int days) throws java.io.IOException {
        com.campus.dto.AdminDashboardChartsResponse charts = getDashboardCharts(days);
        AdminDashboardSummaryResponse summary = getSummary();
        
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"dashboard_analysis_report.csv\"");
        
        response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(response.getOutputStream(), "UTF-8"));
             org.apache.commons.csv.CSVPrinter csvPrinter = new org.apache.commons.csv.CSVPrinter(writer, 
                org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader("Section", "Metric", "Value", "Note").build())) {
            csvPrinter.printRecord("Overview", "Pending Verifications", summary.verification().pendingCount(),
                    "Applications waiting for admin review");
            csvPrinter.printRecord("Overview", "Reviewed Today", summary.verification().reviewedToday(),
                    "Verification applications reviewed today");
            csvPrinter.printRecord("Overview", "Published Posts", summary.community().publishedCount(),
                    "Visible community posts");
            csvPrinter.printRecord("Overview", "Active Jobs", summary.jobs().publishedCount(),
                    "Published job postings");
            csvPrinter.printRecord("Overview", "Published Resources", summary.resources().publishedCount(),
                    "Resources visible to users");
            csvPrinter.printRecord("Overview", "Pending Resources", summary.resources().pendingCount(),
                    "Resources waiting for admin review");

            com.campus.dto.AdminDashboardChartsResponse.RankingData topDownload = charts.getDownloadRankings() == null
                    || charts.getDownloadRankings().isEmpty() ? null : charts.getDownloadRankings().get(0);
            csvPrinter.printRecord("Insight", "Top Download Resource",
                    topDownload == null ? "N/A" : topDownload.getTitle(),
                    topDownload == null ? "No resource download data" : topDownload.getCount() + " downloads");
            com.campus.dto.AdminDashboardChartsResponse.TagData topTag = charts.getTagProportions() == null
                    || charts.getTagProportions().isEmpty() ? null : charts.getTagProportions().get(0);
            csvPrinter.printRecord("Insight", "Top Community Tag",
                    topTag == null ? "N/A" : topTag.getTag(),
                    topTag == null ? "No tag data" : topTag.getCount() + " posts");

            for (com.campus.dto.AdminDashboardChartsResponse.TrendData data : charts.getRegistrationTrends()) {
                csvPrinter.printRecord("Trend", "Registration", data.getCount(), data.getDate());
            }
            for (com.campus.dto.AdminDashboardChartsResponse.TrendData data : charts.getPostTrends()) {
                csvPrinter.printRecord("Trend", "Post", data.getCount(), data.getDate());
            }
            for (com.campus.dto.AdminDashboardChartsResponse.TrendData data : charts.getActiveUserTrends()) {
                csvPrinter.printRecord("Trend", "Active User (DAU Proxy)", data.getCount(), data.getDate());
            }
            for (com.campus.dto.AdminDashboardChartsResponse.TagData data : charts.getTagProportions()) {
                csvPrinter.printRecord("Distribution", "Community Tag", data.getCount(), data.getTag());
            }
            for (com.campus.dto.AdminDashboardChartsResponse.RankingData data : charts.getDownloadRankings()) {
                csvPrinter.printRecord("Ranking", "Resource Downloads", data.getCount(), data.getTitle());
            }
            
            csvPrinter.flush();
        }
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
