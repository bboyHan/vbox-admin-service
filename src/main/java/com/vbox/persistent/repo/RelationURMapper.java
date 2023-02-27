package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.RelationUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RelationURMapper extends BaseMapper<RelationUserRole> {

    @Delete("delete from relation_user_role where uid = #{uid}")
    int deleteByUid(Integer uid);

    @Select("SELECT rid FROM sys_user s LEFT JOIN relation_user_role ur ON s.id = ur.uid WHERE s.id = #{uid}")
    List<Integer> getRidByUid(Integer uid);
}