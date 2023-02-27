package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderCreateParam {

    private String p_account;  //付方账户
    private String p_order_id;  //付方订单id
    private String p_key;  //付方订单id
    private Integer money; //充值金额
    private String notify_url; //回调付方地址
    private String channel_id; //通道id

    private String attach;
    private String pay_ip;
    private Integer pay_type; //支付类型
    private String sign; //签名

}
