package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_decision_school_profile")
public class DecisionSchoolProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String track;

    private String region;

    private String tierLabel;

    private Integer isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

