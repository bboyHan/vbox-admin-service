package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.JoinRoleMenu;
import com.vbox.persistent.entity.Role;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("select r.*, m.id mid, m.status menu_status, m.icon menu_icon, m.menu_name from sys_role r" +
            " left join relation_role_menu rm on r.id = rm.rid" +
            " left join sys_menu m on rm.mid = m.id")
    List<JoinRoleMenu> listRole();

    @Select({"<script>" +
            " select r.*, m.id mid, m.status menu_status, m.icon menu_icon, m.menu_name from sys_role r" +
            " left join relation_role_menu rm on r.id = rm.rid" +
            " left join sys_menu m on rm.mid = m.id" +
            " <when test='roleIds!=null'>" +
            " where r.id in " +
            " <foreach collection='roleIds' item='item' open='(' separator=',' close=')'>" +
            " #{item}" +
            " </foreach>" +
            " </when>" +
            "</script>"})
    List<JoinRoleMenu> listRoleInIds(List<String> roleIds);

    @Select("SELECT role_value FROM sys_role r WHERE r.id IN (SELECT rid FROM relation_user_role ur where ur.uid = #{uid})")
    Set<String> listRoleValueByUid(Integer uid);

    @Insert("insert into sys_role (order_no, role_name, role_value, create_time, remark, status)" +
            " values (#{orderNo}, #{roleName}, #{role_value}, #{create_time}, #{remark}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertRole(Role role);


}
