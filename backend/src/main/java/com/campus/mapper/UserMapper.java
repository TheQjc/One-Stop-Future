package com.campus.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.User;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT id, phone, nickname, real_name, role, status, verification_status, student_id, created_at, updated_at " +
            "FROM t_user WHERE phone = #{phone} LIMIT 1")
    User selectByPhone(@Param("phone") String phone);
}
