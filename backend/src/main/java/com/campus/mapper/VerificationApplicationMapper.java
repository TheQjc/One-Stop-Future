package com.campus.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.VerificationApplication;

public interface VerificationApplicationMapper extends BaseMapper<VerificationApplication> {

    @Select("SELECT id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at " +
            "FROM t_verification_application WHERE user_id = #{userId} AND status = 'PENDING' ORDER BY created_at DESC LIMIT 1")
    VerificationApplication selectPendingByUserId(@Param("userId") Long userId);

    @Select("SELECT id, user_id, real_name, student_id, status, reject_reason, reviewer_id, reviewed_at, created_at, updated_at " +
            "FROM t_verification_application WHERE status = 'PENDING' ORDER BY created_at DESC LIMIT #{limit}")
    List<VerificationApplication> selectLatestPending(@Param("limit") int limit);
}
