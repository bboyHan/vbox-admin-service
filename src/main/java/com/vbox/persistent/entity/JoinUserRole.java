package com.vbox.persistent.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JoinUserRole {

    private Long id;
    private String nickname;
    private String account;
    private String pass;
    private String avatar;
    private Integer gender;
    private LocalDateTime createTime;
    private String remark;
    private Long rid;
    private String roleName;
    private String roleValue;

}
