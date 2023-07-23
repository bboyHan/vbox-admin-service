package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("vbox_channel_account_del")
@Data
public class CAccountDel {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private String acid;
    private Integer cid; //channelId
    private Integer gid; //gatewayId
    private String acAccount;
    private String acPwd;
    private String acRemark;
    private Integer min;
    private Integer max;
    private String ck;
    private Integer dailyLimit;
    private Integer totalLimit;
    private Integer payType;
    private String payDesc;
    private Integer status;
    private Integer sysStatus;
    private Integer softDel;
    private String sysLog;

}
