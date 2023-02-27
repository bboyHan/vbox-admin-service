package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CAccountParam {

    private Integer id;
    private String acid;
    private String c_channel_id;
    private String c_gateway;
    private String ac_account;
    private String ac_pwd;
    private String ac_remark;
    private Integer min;
    private Integer max;
    private String ck;
    private Integer daily_limit;
    private Integer total_limit;
    private Integer payType;
    private Integer status;
    private String payDesc;

}
