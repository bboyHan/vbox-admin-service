package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleParam {

    private Integer id;
    private Integer orderNo;
    private String roleName;
    private String roleValue;
    private LocalDateTime createTime;
    private String status;
    private String remark;
    private List<String> menu;


}
