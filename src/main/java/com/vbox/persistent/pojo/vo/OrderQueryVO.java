package com.vbox.persistent.pojo.vo;

import lombok.Data;

@Data
//TODO
public class OrderQueryVO {
    private String orderId;  //付方订单id
    private Integer status; //支付状态
    private Integer cost; //支付金额
    private String payUrl;
    private String notifyUrl;

}
