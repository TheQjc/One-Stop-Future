package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_resource_version")
public class ResourceVersion {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private Integer versionNo;
    private String changeType;
    private String title;
    private String category;
    private String summary;
    @TableField(updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.ALWAYS)
    private String description;
    private String status;
    private String fileName;
    private String fileExt;
    private String contentType;
    private Long fileSize;
    private String storageKey;
    private Long operatorId;
    private LocalDateTime createdAt;
}
