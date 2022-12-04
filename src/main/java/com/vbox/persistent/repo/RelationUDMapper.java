package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.RelationUserDept;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RelationUDMapper extends BaseMapper<RelationUserDept> {


    @Delete("delete from relation_user_dept where did = #{did}")
    int deleteByDid(Long rid);

    @Delete("delete from relation_user_dept where uid = #{uid}")
    int deleteByUid(Long uid);
}
