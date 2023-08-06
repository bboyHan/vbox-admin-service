package com.vbox.persistent.pojo.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChannelMultiShop {

    private Integer uid;

    private Integer status;
    private String channel;
    private String shopRemark;
    private String money;

    private String openAndClose;
}
