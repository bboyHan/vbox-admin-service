package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_pay_order_event")
@Data
public class PayOrderEvent {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderId;
    private String platformOid;
    private String eventLog;

    private LocalDateTime createTime;

}
