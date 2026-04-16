package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_resource_item")
public class ResourceItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String category;
    private String summary;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;
    private String status;
    private Long uploaderId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long reviewedBy;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String rejectReason;
    private String fileName;
    private String fileExt;
    private String contentType;
    private Long fileSize;
    private String storageKey;
    private Integer downloadCount;
    private Integer favoriteCount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime publishedAt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
