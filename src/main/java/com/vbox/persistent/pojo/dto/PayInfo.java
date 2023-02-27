package com.vbox.persistent.pojo.dto;

import lombok.Data;

@Data
public class PayInfo {

    private String repeat_passport;
    private String gateway;
    private Integer recharge_type;
    private Integer recharge_unit;
    private String game;
    private String channel;
    private String ck;

}
