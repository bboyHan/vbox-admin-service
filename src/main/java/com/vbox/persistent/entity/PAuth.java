package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_pay_auth")
@Data
public class PAuth {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer pid;
    private String secret;
    private String pub;
    private Integer status;
    private LocalDateTime createTime;

}
