package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_assessment_option")
public class DecisionAssessmentOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private String code;

    private String label;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    private Integer displayOrder;

    private Integer careerScore;

    private Integer examScore;

    private Integer abroadScore;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

