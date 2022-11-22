package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginParam {

    private String username;
    private String password;
    private String captcha;
    private Integer loginType;

}
