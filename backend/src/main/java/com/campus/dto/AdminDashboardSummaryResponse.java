package com.campus.dto;

import java.util.List;

public record AdminDashboardSummaryResponse(
        VerificationSection verification,
        CommunitySection community,
        JobsSection jobs,
        ResourcesSection resources) {

    public record VerificationSection(
            int pendingCount,
            int reviewedToday,
            List<AdminVerificationDashboardResponse.VerificationApplicationSummary> latestPendingApplications) {
    }

    public record CommunitySection(
            int totalCount,
            int publishedCount,
            int hiddenCount,
            int deletedCount,
            List<AdminCommunityPostListResponse.PostItem> latestPosts) {
    }

    public record JobsSection(
            int totalCount,
            int draftCount,
            int publishedCount,
            int offlineCount,
            List<AdminJobListResponse.JobItem> latestActionableJobs) {
    }

    public record ResourcesSection(
            int totalCount,
            int pendingCount,
            int publishedCount,
            int closedCount,
            List<AdminResourceListResponse.ResourceItem> latestPendingResources) {
    }
}
