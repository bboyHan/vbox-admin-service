package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayNotifyVO {

    private Integer status;
    private String order_id;
    private Integer cost;
    private String p_account;
    private String sign;
}
