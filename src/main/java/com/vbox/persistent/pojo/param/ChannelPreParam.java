package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChannelPreParam extends PageParam{

    private Integer id;
    private Integer uid;
    private String channel;
    private String acid;
    private String ckid;
    private String acAccount;
    private String platOid;
    private String platParam;
    private String address;
    private Integer money;
    private Integer status;

}
