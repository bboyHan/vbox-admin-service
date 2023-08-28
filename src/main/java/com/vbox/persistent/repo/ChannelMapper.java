package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.Channel;
import com.vbox.persistent.entity.CChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChannelMapper extends BaseMapper<Channel> {

    @Select("SELECT id FROM vbox_channel WHERE type = 1 and c_game = 'tx'")
    List<Integer> listCID2tx();

    @Select("SELECT id FROM vbox_channel WHERE type = 1 and c_game = 'xoy'")
    List<Integer> listCID2XOY();

    @Select("SELECT * FROM vbox_channel WHERE id = #{id}")
    CChannel getChannelById(Integer id);

    @Select("SELECT * FROM vbox_channel WHERE c_channel_id = #{channelId}")
    CChannel getChannelByChannelId(String channelId);

    @Select("SELECT id FROM vbox_channel WHERE c_channel_id = #{channelId}")
    Integer getChannelIdByChannelId(String channelId);

    @Select("SELECT * FROM vbox_channel WHERE type = 1")
    List<CChannel> getChannelShopTypes();

    @Select("SELECT * FROM vbox_channel WHERE type = 2")
    List<CChannel> getChannelPreTypes();
}
