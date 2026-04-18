package com.campus.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.JobApplication;

import lombok.Data;

public interface JobApplicationMapper extends BaseMapper<JobApplication> {

    @Select("""
            SELECT id, job_id, applicant_user_id, resume_id, status,
                   resume_title_snapshot, resume_file_name_snapshot, resume_file_ext_snapshot,
                   resume_content_type_snapshot, resume_file_size_snapshot, resume_storage_key_snapshot,
                   submitted_at, created_at, updated_at
            FROM t_job_application
            WHERE job_id = #{jobId}
              AND applicant_user_id = #{applicantUserId}
            LIMIT 1
            """)
    JobApplication selectByJobIdAndApplicantUserId(@Param("jobId") Long jobId,
            @Param("applicantUserId") Long applicantUserId);

    @Select("""
            SELECT id, user_id, title, file_name, file_ext, content_type, file_size, storage_key, created_at, updated_at
            FROM t_resume
            WHERE id = #{resumeId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    ResumeSnapshotSource selectOwnedResume(@Param("resumeId") Long resumeId, @Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM t_job_application
            WHERE applicant_user_id = #{userId}
            """)
    int countByApplicantUserId(@Param("userId") Long userId);

    @Select("""
            SELECT a.id,
                   a.job_id AS jobId,
                   j.title AS jobTitle,
                   j.company_name AS companyName,
                   j.city AS city,
                   a.status,
                   a.resume_title_snapshot AS resumeTitleSnapshot,
                   a.resume_file_name_snapshot AS resumeFileNameSnapshot,
                   a.submitted_at AS submittedAt
            FROM t_job_application a
            JOIN t_job_posting j ON j.id = a.job_id
            WHERE a.applicant_user_id = #{userId}
            ORDER BY a.submitted_at DESC, a.id DESC
            LIMIT #{limit}
            """)
    List<MyApplicationRow> selectMyApplications(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM t_job_application")
    int countAllApplications();

    @Select("""
            SELECT COUNT(*)
            FROM t_job_application
            WHERE submitted_at >= #{start}
              AND submitted_at < #{end}
            """)
    int countSubmittedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(DISTINCT applicant_user_id) FROM t_job_application")
    int countUniqueApplicants();

    @Select("SELECT COUNT(DISTINCT job_id) FROM t_job_application")
    int countUniqueJobs();

    @Select("""
            SELECT a.id,
                   a.job_id AS jobId,
                   j.title AS jobTitle,
                   j.company_name AS companyName,
                   a.applicant_user_id AS applicantUserId,
                   u.nickname AS applicantNickname,
                   a.resume_file_name_snapshot AS resumeFileNameSnapshot,
                   a.status,
                   a.submitted_at AS submittedAt
            FROM t_job_application a
            JOIN t_job_posting j ON j.id = a.job_id
            JOIN t_user u ON u.id = a.applicant_user_id
            ORDER BY a.submitted_at DESC, a.id DESC
            LIMIT #{limit}
            """)
    List<AdminApplicationRow> selectAdminApplications(@Param("limit") int limit);

    @Data
    class ResumeSnapshotSource {
        private Long id;
        private Long userId;
        private String title;
        private String fileName;
        private String fileExt;
        private String contentType;
        private Long fileSize;
        private String storageKey;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    class MyApplicationRow {
        private Long id;
        private Long jobId;
        private String jobTitle;
        private String companyName;
        private String city;
        private String status;
        private String resumeTitleSnapshot;
        private String resumeFileNameSnapshot;
        private LocalDateTime submittedAt;
    }

    @Data
    class AdminApplicationRow {
        private Long id;
        private Long jobId;
        private String jobTitle;
        private String companyName;
        private Long applicantUserId;
        private String applicantNickname;
        private String resumeFileNameSnapshot;
        private String status;
        private LocalDateTime submittedAt;
    }
}
