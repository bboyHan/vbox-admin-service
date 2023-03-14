package com.vbox.persistent.pojo.vo;

import com.vbox.persistent.entity.Menu;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TotalVO {

    private Integer recharge;
    private Integer balance;
    private Integer countSale;
    private Integer totalCost;
    private Integer totalCostNum;
    private Integer totalNum;
    private Integer todayCost;
    private Integer todayCostNum;
    private Integer todayNum;
}
