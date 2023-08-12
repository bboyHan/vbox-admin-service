package com.vbox.persistent.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class PAOverviewVO {

    private Integer id;
    private String pAccount;
    private String pRemark;
    private Integer countOrder;
    private Integer countPayed;
    private Integer countOrderYesterday;
    private Integer countPayedYesterday;
    private Integer countOrderToday;
    private Integer countPayedToday;
    private Integer sumPayed;
    private Integer sumPayedYesterday;
    private Integer sumPayedToday;
    List<PAOverviewDetailVO> detailVOList;

}
