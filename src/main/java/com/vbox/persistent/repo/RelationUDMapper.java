package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.RelationUserDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RelationUDMapper extends BaseMapper<RelationUserDept> {


    @Delete("delete from relation_role_menu where rid = #{rid}")
    int deleteByRid(Long rid);

    @Delete("delete from relation_role_menu where mid = #{mid}")
    int deleteByMid(Long mid);
}
