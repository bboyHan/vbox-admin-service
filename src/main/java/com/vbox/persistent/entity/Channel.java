package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("vbox_channel")
@Data
public class Channel {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String c_channel_id;
    private String c_game;
    private String c_game_name;
    private String c_channel; //support recharge type
    private String c_channel_name; //support recharge type

}
