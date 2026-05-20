package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.CommunityPost;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;
import com.campus.dto.AdminDashboardChartsResponse;

public interface CommunityPostMapper extends BaseMapper<CommunityPost> {
    @Select("SELECT CAST(created_at AS DATE) as date, COUNT(*) as count FROM t_community_post WHERE created_at >= #{start} GROUP BY CAST(created_at AS DATE) ORDER BY date ASC")
    List<AdminDashboardChartsResponse.TrendData> selectPostTrends(@org.apache.ibatis.annotations.Param("start") LocalDateTime start);

    @Select("SELECT CAST(created_at AS DATE) as date, COUNT(DISTINCT author_id) as count FROM t_community_post WHERE created_at >= #{start} GROUP BY CAST(created_at AS DATE) ORDER BY date ASC")
    List<AdminDashboardChartsResponse.TrendData> selectActiveUserTrends(@org.apache.ibatis.annotations.Param("start") LocalDateTime start);

    @Select("SELECT tag, COUNT(*) as count FROM t_community_post GROUP BY tag ORDER BY count DESC")
    List<AdminDashboardChartsResponse.TagData> selectTagProportions();
}
