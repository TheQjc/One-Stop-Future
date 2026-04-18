package com.campus.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.campus.dto.AnalyticsDistributionRow;
import com.campus.dto.AnalyticsTrendRow;

public interface AnalyticsReadMapper {

    @Select("SELECT COUNT(*) FROM t_community_post WHERE status = 'PUBLISHED'")
    int countPublishedPosts();

    @Select("SELECT COUNT(*) FROM t_job_posting WHERE status = 'PUBLISHED'")
    int countPublishedJobs();

    @Select("SELECT COUNT(*) FROM t_resource_item WHERE status = 'PUBLISHED'")
    int countPublishedResources();

    @Select("SELECT COUNT(*) FROM t_decision_assessment_session")
    int countAssessmentSessions();

    @Select("SELECT DATE(created_at) AS bucket_date, COUNT(*) AS total " +
            "FROM t_community_post WHERE status = 'PUBLISHED' AND created_at >= #{start} " +
            "GROUP BY DATE(created_at)")
    List<AnalyticsTrendRow> summarizePublishedPostTrend(@Param("start") LocalDateTime start);

    @Select("SELECT DATE(published_at) AS bucket_date, COUNT(*) AS total " +
            "FROM t_job_posting " +
            "WHERE status = 'PUBLISHED' AND published_at IS NOT NULL AND published_at >= #{start} " +
            "GROUP BY DATE(published_at)")
    List<AnalyticsTrendRow> summarizePublishedJobTrend(@Param("start") LocalDateTime start);

    @Select("SELECT DATE(published_at) AS bucket_date, COUNT(*) AS total " +
            "FROM t_resource_item " +
            "WHERE status = 'PUBLISHED' AND published_at IS NOT NULL AND published_at >= #{start} " +
            "GROUP BY DATE(published_at)")
    List<AnalyticsTrendRow> summarizePublishedResourceTrend(@Param("start") LocalDateTime start);

    @Select("SELECT session_date AS bucket_date, COUNT(*) AS total " +
            "FROM t_decision_assessment_session WHERE session_date >= #{start} " +
            "GROUP BY session_date")
    List<AnalyticsTrendRow> summarizeAssessmentTrend(@Param("start") LocalDate start);

    @Select("SELECT recommended_track AS track, COUNT(*) AS count FROM (" +
            "  SELECT user_id, recommended_track, " +
            "    ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY session_date DESC, id DESC) AS rn " +
            "  FROM t_decision_assessment_session" +
            ") t WHERE rn = 1 GROUP BY recommended_track")
    List<AnalyticsDistributionRow> summarizeLatestAssessmentDistribution();
}

