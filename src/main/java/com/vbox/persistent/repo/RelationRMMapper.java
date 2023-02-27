package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.RelationRoleMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RelationRMMapper extends BaseMapper<RelationRoleMenu> {


    @Delete("delete from relation_role_menu where rid = #{rid}")
    int deleteByRid(Integer rid);

    @Delete("delete from relation_role_menu where mid = #{mid}")
    int deleteByMid(Integer mid);
}
