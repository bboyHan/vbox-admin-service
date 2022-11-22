package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleMenuVO {

    private Long id;
    private Long orderNo;
    private String roleName;
    private String roleValue;
    private LocalDateTime createTime;
    private List<Long> menu;
    private String remark;
    private String status;
}
