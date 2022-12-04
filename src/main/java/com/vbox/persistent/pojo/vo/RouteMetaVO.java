package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteMetaVO {

    private String title;
    private String frameSrc;
    private Boolean hideChildrenInMenu;
    private Boolean ignoreKeepAlive;
    private Boolean showMenu;
    private Boolean hideMenu;
    private String currentActiveMenu;
    private String icon;

}
