package com.campus.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.campus.entity.CommunityPost;
import com.campus.entity.JobPosting;
import com.campus.entity.ResourceItem;
import com.campus.entity.VerificationApplication;

public interface AdminDashboardReadMapper {

    @Select("SELECT COUNT(*) FROM t_verification_application WHERE status = 'PENDING'")
    int countPendingVerificationApplications();

    @Select("""
            SELECT COUNT(*)
            FROM t_verification_application
            WHERE reviewed_at IS NOT NULL
              AND reviewed_at >= #{start}
              AND reviewed_at < #{end}
            """)
    int countReviewedVerificationApplicationsBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Select("""
            SELECT id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at
            FROM t_verification_application
            WHERE status = 'PENDING'
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<VerificationApplication> selectLatestPendingApplications(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_community_post")
    int countCommunityPosts();

    @Select("SELECT COUNT(*) FROM t_community_post WHERE status = #{status}")
    int countCommunityPostsByStatus(@Param("status") String status);

    @Select("""
            SELECT id, author_id, tag, title, content, status, like_count, comment_count, favorite_count, created_at, updated_at
            FROM t_community_post
            WHERE status IN ('PUBLISHED', 'HIDDEN', 'DELETED')
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<CommunityPost> selectLatestCommunityPosts(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_job_posting WHERE status <> 'DELETED'")
    int countNonDeletedJobs();

    @Select("SELECT COUNT(*) FROM t_job_posting WHERE status = #{status}")
    int countJobsByStatus(@Param("status") String status);

    @Select("""
            SELECT id, title, company_name, city, job_type, education_requirement, source_platform, source_url,
                   summary, content, deadline_at, published_at, status, created_by, updated_by, created_at, updated_at
            FROM t_job_posting
            WHERE status IN ('DRAFT', 'OFFLINE')
            ORDER BY updated_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<JobPosting> selectLatestActionableJobs(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_resource_item")
    int countResources();

    @Select("SELECT COUNT(*) FROM t_resource_item WHERE status = #{status}")
    int countResourcesByStatus(@Param("status") String status);

    @Select("""
            SELECT id, title, category, summary, description, status, uploader_id, reviewed_by, reject_reason,
                   file_name, file_ext, content_type, file_size, storage_key, download_count, favorite_count,
                   published_at, reviewed_at, created_at, updated_at
            FROM t_resource_item
            WHERE status = 'PENDING'
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<ResourceItem> selectLatestPendingResources(@Param("limit") int limit);
}
