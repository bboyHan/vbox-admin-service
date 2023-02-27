package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("vbox_user_wallet")
@Data
public class VboxUserWallet {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private String nickname;
    private Integer recharge;
    private BigDecimal tariff;
    private LocalDateTime createTime;

}
