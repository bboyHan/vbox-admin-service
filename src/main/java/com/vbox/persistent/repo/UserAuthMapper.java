package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.UserAuth;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {

    @Select("select * from sys_user_auth where uid = #{uid}")
    UserAuth getAuthByUid(Integer uid);

    @Select("select a.* from sys_user_auth a,sys_user u where a.uid = u.id and account = #{account}")
    UserAuth getAuthByAccount(String account);

    @Delete("delete from sys_user_auth where uid = #{uid}")
    int deleteByUid(Integer uid);
}
