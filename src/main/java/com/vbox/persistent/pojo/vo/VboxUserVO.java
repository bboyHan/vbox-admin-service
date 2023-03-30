package com.vbox.persistent.pojo.vo;

import lombok.Data;

@Data
public class VboxUserVO {

    private Integer id;
    private String nickname;
    private String account;
    private Integer balance;
    private Integer userLevel;
    private Integer yesterdayOrderNum;
    private Integer yesterdayProdOrderNum;
    private Integer yesterdayOrderSum;

    private Integer todayOrderNum;
    private Integer todayOrderSum;
    private Integer todayProdOrderNum;

    private Integer hourOrderSum;
    private Integer hourOrderNum;
    private Integer hourProdOrderNum;

    private Integer totalCostNum;
    private Integer totalCostSum;
    private Integer totalProdNum;
}
