package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_menu")
@Data
public class Menu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pid;
    private Long orderNo;
    private Long type;
    private String menuName;
    private String routePath;
    private Integer isShow;
    private Integer isExt;
    private Integer keepAlive;
    private String component;
    private String icon;
    private String status;
    private String permission;
    private LocalDateTime createTime;

}
