package com.campus.document;

import com.campus.common.SearchContentType;
import com.campus.common.SearchVisibility;
import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentBuilder {

    public static UnifiedSearchDocument fromCommunityPost(CommunityPost post, String authorName) {
        return UnifiedSearchDocument.of(
                "post_" + post.getId(),
                SearchContentType.POST.code(),
                post.getAuthorId(),
                SearchVisibility.PUBLIC.value(),
                post.getStatus(),
                post.getTitle(),
                post.getContent(),
                truncate(post.getContent(), 200),
                post.getTag() != null ? List.of(post.getTag()) : List.of(),
                authorName,
                post.getAuthorId(),
                post.getCreatedAt(),
                post.getCreatedAt(),
                "/community/" + post.getId()
        );
    }

    public static UnifiedSearchDocument fromJobPosting(JobPosting job) {
        return UnifiedSearchDocument.of(
                "job_" + job.getId(),
                SearchContentType.JOB.code(),
                job.getCreatedBy(),
                SearchVisibility.PUBLIC.value(),
                job.getStatus(),
                job.getTitle(),
                job.getContent(),
                job.getSummary(),
                List.of(job.getCity() != null ? job.getCity() : "", job.getJobType() != null ? job.getJobType() : ""),
                job.getCompanyName(),
                job.getCreatedBy(),
                job.getPublishedAt(),
                job.getCreatedAt(),
                "/jobs/" + job.getId()
        );
    }

    public static UnifiedSearchDocument fromResourceItem(ResourceItem resource, String uploaderName) {
        return UnifiedSearchDocument.of(
                "resource_" + resource.getId(),
                SearchContentType.RESOURCE.code(),
                resource.getUploaderId(),
                SearchVisibility.PUBLIC.value(),
                resource.getStatus(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getSummary(),
                resource.getCategory() != null ? List.of(resource.getCategory()) : List.of(),
                uploaderName,
                resource.getUploaderId(),
                resource.getPublishedAt(),
                resource.getCreatedAt(),
                "/resources/" + resource.getId()
        );
    }

    public static UnifiedSearchDocument forPrivateContent(
            String idPrefix,
            Long id,
            SearchContentType contentType,
            Long ownerId,
            String title,
            String content,
            String summary,
            LocalDateTime createdAt,
            String path) {
        return UnifiedSearchDocument.of(
                idPrefix + "_" + id,
                contentType.code(),
                ownerId,
                SearchVisibility.PRIVATE.value(),
                null,
                title,
                content,
                summary,
                List.of(),
                null,
                ownerId,
                createdAt,
                createdAt,
                path
        );
    }

    private static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
