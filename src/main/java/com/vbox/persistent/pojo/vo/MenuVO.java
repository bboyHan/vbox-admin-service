package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuVO {

    private Integer id;
    private Integer pid;
    private Integer parentMenu;
    private Integer orderNo;
    private Integer type;
    private String menuName;
    private String component;
    private String routePath;
    private String title;
    private String icon;
    private String status;
    private String permission;
    private LocalDateTime createTime;
    private List<MenuVO> children;

}
