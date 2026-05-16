package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.CommunityPost;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import com.campus.dto.AdminDashboardChartsResponse;

public interface CommunityPostMapper extends BaseMapper<CommunityPost> {
    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM t_community_post WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date ASC")
    List<AdminDashboardChartsResponse.TrendData> selectPostTrends(@org.apache.ibatis.annotations.Param("days") int days);

    @Select("SELECT DATE(created_at) as date, COUNT(DISTINCT author_id) as count FROM t_community_post WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date ASC")
    List<AdminDashboardChartsResponse.TrendData> selectActiveUserTrends(@org.apache.ibatis.annotations.Param("days") int days);

    @Select("SELECT tag, COUNT(*) as count FROM t_community_post GROUP BY tag ORDER BY count DESC")
    List<AdminDashboardChartsResponse.TagData> selectTagProportions();
}
