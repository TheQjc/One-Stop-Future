package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_school_metric_definition")
public class DecisionSchoolMetricDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String track;

    private String metricCode;

    private String metricLabel;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String metricUnit;

    private String valueType;

    private Integer chartable;

    private Integer metricOrder;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

