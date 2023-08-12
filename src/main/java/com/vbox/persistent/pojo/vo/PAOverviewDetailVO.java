package com.vbox.persistent.pojo.vo;

import lombok.Data;

@Data
public class PAOverviewDetailVO {

    private Integer id;
    private String channel;
    private String pAccount;
    private String pRemark;
    private Integer countOrderYesterday;
    private Integer countPayedYesterday;
    private Integer countOrderToday;
    private Integer countPayedToday;
    private Integer sumPayedYesterday;
    private Integer sumPayedToday;


}
