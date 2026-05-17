package com.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.ResourceItem;

public interface ResourceItemMapper extends BaseMapper<ResourceItem> {
    @org.apache.ibatis.annotations.Select("SELECT title, download_count as count FROM t_resource_item ORDER BY download_count DESC LIMIT 10")
    java.util.List<com.campus.dto.AdminDashboardChartsResponse.RankingData> selectDownloadRankings();
}
