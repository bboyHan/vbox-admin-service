package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_channel_shop")
@Data
public class ChannelShop {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer cid;
    private String channel;
    private String shopRemark;
    private String address;
    private Integer money;
    private Integer status;
    private LocalDateTime createTime; //support recharge type

}
