package com.vbox.persistent.pojo.param;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserSubCreateOrUpdParam {

    private String account;
    private String pass;
    private BigDecimal charge;
    private BigDecimal min;
    private BigDecimal max;
    private Integer status;
//    private Integer deptId;
//    private Integer roleId;

}
