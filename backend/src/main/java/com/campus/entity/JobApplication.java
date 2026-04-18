package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_job_application")
public class JobApplication {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long applicantUserId;
    private Long resumeId;
    private String status;
    private String resumeTitleSnapshot;
    private String resumeFileNameSnapshot;
    private String resumeFileExtSnapshot;
    private String resumeContentTypeSnapshot;
    private Long resumeFileSizeSnapshot;
    private String resumeStorageKeySnapshot;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
