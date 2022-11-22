package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MenuVO {

    private Long id;
    private Long pid;
    private Long parentMenu;
    private Long orderNo;
    private Long type;
    private String menuName;
    private String component;
    private String icon;
    private String status;
    private String permission;
    private LocalDateTime createTime;
    private List<MenuVO> children;

}
