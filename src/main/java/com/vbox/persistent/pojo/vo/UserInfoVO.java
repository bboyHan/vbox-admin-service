package com.vbox.persistent.pojo.vo;

import com.vbox.persistent.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserInfoVO {

    private String token;

    private Long id;
    private String nickname;
    private String account;
    private String avatar;
    private String gender;
    private List<Role> role;
    private LocalDateTime createTime;
    private String remark;

    //ext
    private String realName;
    private String country;
    private String desc;
    private String introduction;
    private String signature;
    private String address;
    private String phone;
}
