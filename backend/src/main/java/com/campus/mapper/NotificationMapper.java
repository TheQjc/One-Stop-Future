package com.campus.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.Notification;

public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM t_notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(@Param("userId") Long userId);

    @Select("SELECT id, user_id, type, title, content, is_read, source_type, source_id, created_at, read_at " +
            "FROM t_notification WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<Notification> selectLatestByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE t_notification SET is_read = 1, read_at = #{readAt} " +
            "WHERE id = #{id} AND user_id = #{userId} AND is_read = 0")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Update("UPDATE t_notification SET is_read = 1, read_at = #{readAt} WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
