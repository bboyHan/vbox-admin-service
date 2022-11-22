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

    @Delete("delete from sys_menu where pid = #{pid}")
    int deleteChildMenus(Long pid);

    @Select("SELECT 1 FROM sys_menu WHERE pid=#{id} LIMIT 1")
    Integer isExistCMenu(Long pid);

}
