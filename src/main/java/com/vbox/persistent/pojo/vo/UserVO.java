package com.vbox.persistent.pojo.vo;

import com.vbox.persistent.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO {

    private Long id;
    private String nickname;
    private String account;
    private String avatar;
    private String gender;
    private String roles;
    private LocalDateTime createTime;
    private String remark;
}
