package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.Location;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {

    @Select("select * from sys_location where region like concat('%',#{region},'%') limit 1")
    Location regionSearch(String region);
}
