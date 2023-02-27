package com.vbox.persistent.pojo.vo;

import lombok.Data;

@Data
//TODO
public class OrderCallbackVO {

    private String p_order_id;  //付方订单id
    private Integer pay_status; //支付状态
    private String pay_desc; //支付状态描述
    private String msg;

}
