package com.vbox.persistent.pojo.vo;

import lombok.Data;

@Data
public class PayOrderCreateVO {

    private Integer status;
    private String orderId;
    private Integer cost;
    private String payUrl;
    private String attach;
}
