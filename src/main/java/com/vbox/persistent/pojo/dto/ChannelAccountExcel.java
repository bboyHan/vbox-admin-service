package com.vbox.persistent.pojo.dto;

import lombok.Data;

@Data
public class ChannelAccountExcel {

    private String c_channel_id;
    private String c_gateway;
    private String ac_remark;
    private String ac_account;
    private String ac_pwd;
    private Integer min;
    private Integer max;
    private String ck;
    private Integer daily_limit;
    private Integer total_limit;
    private Integer payType;

}
