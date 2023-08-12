package com.vbox.persistent.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChannelMultiTreeShop {

    public ChannelMultiTreeShop(){

    }
    private String id;
    private Integer uid;

    private Integer status;
    private String channel;
    private String shopRemark;
    private String money;

    private String openAndClose;
    private List<ChannelMultiShop> children;
}
