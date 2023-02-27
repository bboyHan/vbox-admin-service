package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_pay_account")
@Data
public class PAccount {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String pAccount;
    private String pKey;
    private String pRemark;
    private Integer status;
    private LocalDateTime createTime;


}
