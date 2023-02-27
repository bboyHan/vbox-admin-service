package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@TableName("vbox_channel_gateway")
@Data
public class CGateway {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer cid;
    private String c_gateway;
    private String c_gateway_name;
    private String s_recharge_type; //support recharge type

}
