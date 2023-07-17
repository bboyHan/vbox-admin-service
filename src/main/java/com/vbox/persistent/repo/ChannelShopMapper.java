package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.ChannelShop;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChannelShopMapper extends BaseMapper<ChannelShop> {

    @Select("select money from vbox_channel_shop where channel = #{channel} and status = 1")
    List<Integer> getChannelShopMoneyList(String channel);

}
