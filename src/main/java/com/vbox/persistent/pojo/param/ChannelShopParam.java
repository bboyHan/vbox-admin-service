package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChannelShopParam extends PageParam{

    private Integer id;
    private Integer uid;
    private String channel;
    private String shopRemark;
    private String address;
    private Integer money;
    private Integer status;

}
