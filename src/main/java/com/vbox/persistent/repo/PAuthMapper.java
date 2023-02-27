package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.PAuth;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PAuthMapper extends BaseMapper<PAuth> {

    @Delete("delete from vbox_pay_auth where pid = #{pid}")
    int deleteByPid(Integer pid);
}
