package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayOrderVO {

    private Integer id;
    private String orderId;
    private String pa;
    private Integer cost;
    private String acAccount;
    private String acRemark;
    private String platformOid;
    private String channel;
    private String resourceUrl;
    private String payIp;
    private String payRegion;
    private Integer orderStatus;
    private Integer codeUseStatus;
    private Integer callbackStatus;
    private LocalDateTime createTime;
    private LocalDateTime asyncTime;
    private LocalDateTime callTime;
}
