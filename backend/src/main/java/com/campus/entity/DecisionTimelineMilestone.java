package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_timeline_milestone")
public class DecisionTimelineMilestone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String track;

    private String phaseCode;

    private String phaseLabel;

    private String title;

    private String summary;

    private Integer offsetMonths;

    private Integer offsetDays;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String actionChecklist;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String resourceHint;

    private Integer displayOrder;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

