package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PAccountVO {

    private Integer id;
    private String p_account;
    private String p_key;
    private String p_remark;
    private String pub;
    private String secret;
    private Integer status;
    private LocalDateTime create_time;

}
