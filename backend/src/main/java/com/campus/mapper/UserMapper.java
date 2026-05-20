package com.campus.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import com.campus.dto.AdminDashboardChartsResponse;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.User;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT id, phone, nickname, real_name, role, status, verification_status, student_id, created_at, updated_at " +
            "FROM t_user WHERE phone = #{phone} LIMIT 1")
    User selectByPhone(@Param("phone") String phone);
    @Select("SELECT CAST(created_at AS DATE) as date, COUNT(*) as count FROM t_user WHERE created_at >= #{start} GROUP BY CAST(created_at AS DATE) ORDER BY date ASC")
    List<AdminDashboardChartsResponse.TrendData> selectRegistrationTrends(@Param("start") LocalDateTime start);
}
