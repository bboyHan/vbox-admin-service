package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_user_ext")
@Data
public class UserExt {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uid;
    private String realName;
    private String country;
    private String desc;
    private String introduction;
    private String signature;
    private String address;
    private String phone;

}
