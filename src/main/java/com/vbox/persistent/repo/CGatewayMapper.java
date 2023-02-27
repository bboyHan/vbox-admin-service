package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.CGateway;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CGatewayMapper extends BaseMapper<CGateway> {

    @Select("SELECT c.c_channel_id, c.c_game, c.c_game_name, c.c_channel, c.c_channel_name, g.id AS id,g.cid, g.c_gateway, g.c_gateway_name, g.s_recharge_type FROM vbox_channel c LEFT JOIN vbox_channel_gateway g ON c.id = g.cid WHERE c.c_channel_id = #{channelId}")
    List<CGatewayInfo> getGatewayListByCId(String channelId);

    @Select("SELECT c.c_channel_id, c.c_game, c.c_game_name, c.c_channel, c.c_channel_name, g.id AS id,g.cid, g.c_gateway, g.c_gateway_name, g.s_recharge_type FROM vbox_channel c LEFT JOIN vbox_channel_gateway g ON c.id = g.cid WHERE c.c_channel_id = #{channelId} AND g.c_gateway = #{gateway}")
    CGatewayInfo getGateWayInfoByCIdAndCG(String channelId, String gateway);

    @Select("SELECT c.c_channel_id, c.c_game, c.c_game_name, c.c_channel, c.c_channel_name, g.id AS id,g.cid, g.c_gateway, g.c_gateway_name, g.s_recharge_type FROM vbox_channel c LEFT JOIN vbox_channel_gateway g ON c.id = g.cid WHERE g.cid = #{cid} AND g.id = #{gid}")
    CGatewayInfo getGateWayInfoByCIdAndGId(Integer cid, Integer gid);

}
