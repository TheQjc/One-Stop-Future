package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_school_metric")
public class DecisionSchoolMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long schoolId;

    private String metricCode;

    private String metricValue;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

