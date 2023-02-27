package com.vbox.persistent.pojo.dto;

import lombok.Data;

@Data
public class POrderQueue {

    private String pa;
    private Integer channel;
    private String orderId;
    private Integer reqMoney;
    private String notify;
    private String attach;
    private Integer payType;

}
