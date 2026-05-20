package com.campus.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.entity.ResourceVersion;

public interface ResourceVersionMapper extends BaseMapper<ResourceVersion> {

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM t_resource_version WHERE resource_id = #{resourceId}")
    int selectMaxVersionNo(@Param("resourceId") Long resourceId);
}
