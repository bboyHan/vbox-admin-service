package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.UserLogin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserLoginMapper extends BaseMapper<UserLogin> {

    @Select("SELECT * FROM sys_user_login l where username = #{username} and captcha = #{captcha} and login_type = #{loginType}")
    UserLogin validateLogin(UserLogin userLogin);

    @Select("SELECT 1 FROM sys_user WHERE account=#{account} LIMIT 1")
    Integer isExistAccount(String account);

    @Select("SELECT remark FROM sys_user_login WHERE username=#{username} LIMIT 1")
    String pubKey(String username);

}
