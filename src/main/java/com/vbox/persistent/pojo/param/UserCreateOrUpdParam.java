package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCreateOrUpdParam {

    private Long id;
    private Long roleId;
    private Long deptId;
    private String account;
    private Integer gender;
    private String pass;
    private LocalDateTime createTime;

}
