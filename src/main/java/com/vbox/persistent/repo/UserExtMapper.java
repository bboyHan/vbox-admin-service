package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.entity.UserExt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserExtMapper extends BaseMapper<UserExt> {

    @Select("SELECT * FROM sys_user WHERE account = #{account}")
    User getUserByAccount(String account);

    @Select("SELECT * FROM sys_user_ext WHERE uid = #{uid}")
    UserExt getUserInfoByUid(Integer uid);

}
