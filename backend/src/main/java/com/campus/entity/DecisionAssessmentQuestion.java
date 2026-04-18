package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_assessment_question")
public class DecisionAssessmentQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String prompt;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    private Integer displayOrder;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

