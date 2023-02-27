package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCreateOrUpdParam {

    private Integer id;
    private Integer roleId;
    private Integer deptId;
    private String account;
    private Integer gender;
    private String pass;
    private LocalDateTime createTime;

}
