package com.campus.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_community_comment")
public class CommunityComment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long authorId;
    private Long parentCommentId;
    private Long replyToUserId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
