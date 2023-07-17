package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("vbox_channel")
@Data
public class CChannel {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String cChannelId;
    private String cGame;
    private String cGameName;
    private String cChannel; //support recharge type
    private String cChannelName; //support recharge type

}
