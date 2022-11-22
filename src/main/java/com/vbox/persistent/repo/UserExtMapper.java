package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.UserExt;
import com.vbox.persistent.pojo.vo.UserInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserExtMapper extends BaseMapper<UserExt> {

    @Select("SELECT * FROM sys_user u" +
            " LEFT JOIN sys_user_ext e ON e.uid = u.id WHERE u.account = #{account}")
    UserInfoVO getUserInfoByAccount(String account);

}
