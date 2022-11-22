package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserParam {

    private Long id;
    private Long roleId;
    private Long deptId;
    private String nickname;
    private String email;
    private String remark;
    private LocalDateTime createTime;

}
