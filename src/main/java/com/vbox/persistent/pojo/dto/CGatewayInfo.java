package com.vbox.persistent.pojo.dto;

import lombok.Data;

@Data
public class CGatewayInfo {

    private Integer id;
    private Integer cid;
    private String CGame;
    private String CGameName;
    private String CChannel;
    private String CChannelId;
    private String CChannelName;
    private String CGateway;
    private String CGatewayName;
    private String SRechargeType; //support recharge type

}
