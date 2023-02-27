package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.Menu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("select * from sys_menu")
    List<Menu> listMenu();

    @Select("<script>" +
            " select * from sys_menu" +
            " <when test='ids!=null'>" +
            " where id in " +
            " <foreach collection='ids' item='item' open='(' separator=',' close=')'>" +
            " #{item}" +
            " </foreach>" +
            " </when>" +
            "</script>")
    List<Menu> listMenuInIds(List<String> ids);

    @Delete("delete from sys_menu where pid = #{pid}")
    int deleteChildMenus(Integer pid);

    @Select("SELECT 1 FROM sys_menu WHERE pid=#{id} LIMIT 1")
    Integer isExistCMenu(Integer pid);

}
