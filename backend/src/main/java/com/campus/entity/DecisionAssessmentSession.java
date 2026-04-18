package com.campus.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_assessment_session")
public class DecisionAssessmentSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String recommendedTrack;

    private Integer careerScore;

    private Integer examScore;

    private Integer abroadScore;

    private String summaryText;

    private LocalDate sessionDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

