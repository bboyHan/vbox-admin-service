package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.entity.JoinUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT u.*,r.id rid, r.role_name, r.role_value FROM sys_user u" +
            " LEFT JOIN relation_user_role ur ON u.id = ur.uid" +
            " LEFT JOIN sys_role r ON ur.rid = r.id")
    List<JoinUserRole> listUser();

    @Select("SELECT 1 FROM sys_user WHERE account=#{account} LIMIT 1")
    Integer isExistAccount(String account);

    @Select("SELECT u.*,r.id rid, r.role_name, r.role_value FROM sys_user u" +
            " LEFT JOIN relation_user_role ur ON u.id = ur.uid" +
            " LEFT JOIN sys_role r ON ur.rid = r.id WHERE u.account = #{account}")
    List<JoinUserRole> getUserByUserName(String account);

}
