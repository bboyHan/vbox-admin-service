package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.RelationUserSub;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RelationUSMapper extends BaseMapper<RelationUserSub> {

    @Delete("delete from relation_user_sub where uid = #{rid}")
    int deleteByUid(Integer rid);

    @Delete("delete from relation_user_sub where sid = #{sid}")
    int deleteBySid(Integer sid);

    @Select("select sid from relation_user_sub where uid = #{uid}")
    List<Integer> listSidByUid(Integer uid);
}
