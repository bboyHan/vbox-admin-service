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
    private Integer id;
    private Integer pid;
    private Integer orderNo;
    private Integer type;
    private String menuName;
    private String routePath;
    private String title;
    private String redirect;
    private String currentActiveMenu;
    private String frameSrc;
    private Integer isShow;
    private Integer isHide;
    private Integer isExt;
    private Integer keepAlive;
    private Integer isBreadcrumb;
    private Integer hideChildrenInMenu;
    private String component;
    private String icon;
    private String status;
    private String permission;
    private LocalDateTime createTime;

}
