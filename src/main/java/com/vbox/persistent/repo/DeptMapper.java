package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptMapper  extends BaseMapper<Dept> {

    @Select("select * from sys_dept")
    List<Dept> listDept();

    @Select("select count(1) from sys_dept where pid = #{id}")
    int countByPid(Integer id);
}
