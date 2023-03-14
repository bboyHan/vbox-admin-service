package com.vbox.persistent.pojo.vo;

import lombok.Data;

@Data
public class SaleVO {

    private Integer id;
    private String nickname;
    private String account;
    private Integer balance;
    private Integer countCA;
    private Integer countEnableCA;
    private Integer totalCost;
    private Integer totalCostNum;
    private Integer totalNum;
    private Integer todayCost;
    private Integer todayCostNum;
    private Integer todayNum;
}
