package com.vbox.persistent.pojo.param;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PAOverviewParam extends PageParam{

    private Integer id;
    private String acid;
    private String p_account;
    private String p_remark;
    private String order_id;
    private Integer status;

}
