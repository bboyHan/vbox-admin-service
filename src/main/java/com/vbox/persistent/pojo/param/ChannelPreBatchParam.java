package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChannelPreBatchParam extends PageParam{

    private Integer id;
    private Integer uid;
    private String channel;
    private String acid;
    private String acRemark;
    private String acAccount;
    private Integer count;
    private Integer money;

}
