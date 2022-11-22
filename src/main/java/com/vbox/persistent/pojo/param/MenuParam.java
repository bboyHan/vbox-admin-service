package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MenuParam {

    private Long id;
    private Long pid;
    private Long parentMenu;
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
