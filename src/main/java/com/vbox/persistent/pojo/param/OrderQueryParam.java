package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderQueryParam extends PageParam {

    private String orderStatus;
    private String callbackStatus;
    private String orderId;    //付方订单号
    private String p_account;  //付方ID
    private String cChannelId;
    private String acAccount;
    private String acRemark;

}
