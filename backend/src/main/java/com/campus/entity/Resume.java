package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String fileName;
    private String fileExt;
    private String contentType;
    private Long fileSize;
    private String storageKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
