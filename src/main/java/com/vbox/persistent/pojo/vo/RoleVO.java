package com.vbox.persistent.pojo.vo;

import com.vbox.persistent.entity.Menu;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleVO {

    private Integer id;
    private Integer orderNo;
    private String roleName;
    private String roleValue;
    private LocalDateTime createTime;
    private String status;
    private String remark;
    private List<Menu> menu;
}
