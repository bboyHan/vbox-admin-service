package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_channel_acwallet")
@Data
public class CAccountWallet {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer caid;
    private Integer cost;
    private String oid;
    private LocalDateTime createTime;

}
