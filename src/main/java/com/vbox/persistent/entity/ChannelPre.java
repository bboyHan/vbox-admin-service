package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_channel_pre_code")
@Data
public class ChannelPre {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer cid;
    private String acid;
    private String ckid;
    private String acAccount;
    private String channel;
    private String platOid;
    private String platParam;
    private String address;
    private String remark;
    private Integer money;
    private Integer status;
    private LocalDateTime createTime;

}
