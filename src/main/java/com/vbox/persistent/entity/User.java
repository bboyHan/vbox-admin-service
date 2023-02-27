package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_user")
@Data
public class User {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String nickname;
    private String account;
    private LocalDateTime create_time;
    private String remark;

}
