package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_pay_order")
@Data
public class PayOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderId;
    private String pAccount;
    private Integer cost;
    private String acId;
    private String platformOid;
    private String cChannelId;
    private String resourceUrl;
    private String notifyUrl;
    private Integer orderStatus;
    private Integer callbackStatus;
    private Integer codeUseStatus;
    private LocalDateTime createTime;

}
