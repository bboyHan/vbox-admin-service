package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteVO {

    private Integer id;
    private Integer pid;
    private Integer orderNo;
    private String path;
    private String component;
    private String name;
    private String redirect;
    private String permission;
    private RouteMetaVO meta;
    private List<RouteVO> children;

}
