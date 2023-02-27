package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MenuParam {

    private Integer id;
    private Integer pid;
    private Integer parentMenu;
    private Integer orderNo;
    private Integer type;
    private String menuName;
    private String title;
    private String frameSrc;
    private String routePath;
    private Integer isShow;
    private Integer isHide;
    private Integer isExt;
    private Integer isBreadcrumb;
    private Integer keepAlive;
    private Integer hideChildrenInMenu;
    private String redirect;
    private String currentActiveMenu;
    private String component;
    private String icon;
    private String status;
    private String permission;
    private LocalDateTime createTime;

}
