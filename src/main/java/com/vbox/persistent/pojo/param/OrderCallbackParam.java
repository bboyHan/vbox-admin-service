package com.vbox.persistent.pojo.param;

import lombok.Data;

@Data
public class OrderCallbackParam {

    private String p_account;  //付方账户
    private String p_key;  //付方key
    private String p_order_id;  //付方订单id
    private Integer pay_status; //支付状态

}
