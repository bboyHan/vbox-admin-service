package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.Channel;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {

    @Select("SELECT * FROM vbox_channel WHERE c_channel_id = #{channelId}")
    Channel getChannelByChannelId(String channelId);

    @Select("SELECT id FROM vbox_channel WHERE c_channel_id = #{channelId}")
    Integer getChannelIdByChannelId(String channelId);

}
