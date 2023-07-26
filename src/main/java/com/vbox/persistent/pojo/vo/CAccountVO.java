package com.vbox.persistent.pojo.vo;

import com.vbox.common.enums.EnableEnum;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import lombok.Data;

@Data
public class CAccountVO {

    private Integer id;
    private String acid;
    private String c_channel_id;
    private String c_channel_name;
    private String c_gateway_name;
    private String ac_remark;
    private String ac_account;
    private String ac_pwd;
    private String pay_desc;
    private Integer daily_limit;
    private Integer total_limit;
    private Integer today_cost;
    private Integer yesterday_cost;
    private Integer before_day_cost;
    private Integer total_cost;
    private Integer min;
    private Integer max;
    private Integer status;
    private Integer pre_count;
    private Integer sys_status;
    private String sys_log;

    private String sale_name;


    //

    public static CAccountVO transfer(CAccount ca) {
        CAccountVO vo = new CAccountVO();
        vo.setId(ca.getId());
        vo.setAc_account(ca.getAcAccount());
        vo.setAcid(ca.getAcid());
        vo.setStatus(EnableEnum.valid(ca.getStatus()));
        vo.setMin(ca.getMin());
        vo.setMax(ca.getMax());
        vo.setDaily_limit(ca.getDailyLimit());
        vo.setTotal_limit(ca.getTotalLimit());
        vo.setAc_remark(ca.getAcRemark());
        vo.setPay_desc(ca.getPayDesc());
        vo.setSys_log(ca.getSysLog());
        vo.setSys_status(ca.getSysStatus());
        return vo;
    }
}
