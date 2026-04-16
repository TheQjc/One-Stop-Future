package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_job_posting")
public class JobPosting {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String companyName;
    private String city;
    private String jobType;
    private String educationRequirement;
    private String sourcePlatform;
    private String sourceUrl;
    private String summary;
    private String content;
    private LocalDateTime deadlineAt;
    private LocalDateTime publishedAt;
    private String status;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
