package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChannelPreBatchAcListParam extends PageParam{

    private Integer id;
    private Integer uid;
    private String channel;
    private List<String> acidList;
    private Integer count;
    private Integer money;

}
