package com.vbox.persistent.pojo.param;

import lombok.Data;

@Data
public class OrderQueryParam {

    private String p_account;  //付方账户
    private String p_key;  //付方key
    private String p_order_id;  //付方订单号
    private String sign; //签名

}
