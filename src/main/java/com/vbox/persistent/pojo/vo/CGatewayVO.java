package com.vbox.persistent.pojo.vo;

import com.vbox.persistent.pojo.dto.CGatewayInfo;
import lombok.Data;

@Data
public class CGatewayVO {

    private Integer id;
    private Integer cid;
    private String c_game;
    private String c_game_name;
    private String c_channel;
    private String c_channel_name;
    private String c_gateway;
    private String c_gateway_name;
    private String s_recharge_type; //support recharge type


    public static CGatewayVO transfer(CGatewayInfo cgi) {
        CGatewayVO vo = new CGatewayVO();
        vo.setC_gateway(cgi.getCGateway());
        vo.setId(cgi.getId());
        vo.setCid(cgi.getCid());
        vo.setC_game(cgi.getCGame());
        vo.setC_game_name(cgi.getCGameName());
        vo.setC_channel(cgi.getCChannel());
        vo.setC_channel_name(cgi.getCChannelName());
        vo.setC_gateway_name(cgi.getCGatewayName());
        vo.setS_recharge_type(cgi.getSRechargeType());
        return vo;
    }
}
