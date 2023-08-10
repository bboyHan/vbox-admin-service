package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.VboxProxy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VboxProxyMapper extends BaseMapper<VboxProxy> {

    @Select("select url from vbox_proxy where chan = #{chan} limit 1")
    String getEnvUrl(String chan);
}
