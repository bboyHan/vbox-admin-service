package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteVO {

    private Long id;
    private Long pid;
    private String path;
    private String component;
    private String name;
    private String redirect;
    private String permission;
    private RouteMetaVO meta;
    private List<RouteVO> children;

}
