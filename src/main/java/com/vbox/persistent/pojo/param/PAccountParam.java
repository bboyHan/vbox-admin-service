package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PAccountParam {

    private Integer id;
    private String acid;
    private String p_account;
    private String p_remark;
    private Integer status;

}
