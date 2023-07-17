package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TxCAccountParam extends PageParam{

    private Integer id;
    private String acid;
    private String c_channel_id;
    private String c_gateway;
    private String ac_account;
    private String openId;
    private String openKey;
    private String ck;
    private String ac_remark;
    private Integer min;
    private Integer max;
    private Integer daily_limit;
    private Integer total_limit;
    private Integer payType;
    private Integer status;
    private String payDesc;

}
