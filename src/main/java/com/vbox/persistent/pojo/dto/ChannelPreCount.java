package com.vbox.persistent.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChannelPreCount {

    private Integer count;
    private String acid;
    private String acAccount;
    private Integer status;

}
