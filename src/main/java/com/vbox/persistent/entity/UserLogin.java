package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_user_login")
@Data
public class UserLogin {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private String username;
    private Integer loginType;
    private String captcha;
    private String remark;
    private LocalDateTime createTime;

}
