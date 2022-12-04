package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_user_auth")
@Data
public class UserAuth {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uid;
    private Integer status;
    private String secret;
    private String pub;
    private LocalDateTime createTime;

}
