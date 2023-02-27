package com.vbox.persistent.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
public class CAccountInfo {

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
    private String sysLog;

    // CAccountWallet
    private Integer wid;
    private Integer caid;
    private Integer cost;
    private String oid;
    private LocalDateTime createTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CAccountInfo that = (CAccountInfo) o;
        return Objects.equals(id, that.id) && Objects.equals(uid, that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, uid);
    }
}
