package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.RelationUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RelationURMapper extends BaseMapper<RelationUserRole> {


    @Delete("delete from relation_role_menu where rid = #{rid}")
    int deleteByRid(Long rid);

    @Delete("delete from relation_role_menu where mid = #{mid}")
    int deleteByMid(Long mid);
}
