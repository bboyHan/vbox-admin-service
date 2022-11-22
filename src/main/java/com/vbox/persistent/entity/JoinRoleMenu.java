package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JoinRoleMenu {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderNo;
    private String roleName;
    private String roleValue;
    private LocalDateTime createTime;
    private String status;
    private String remark;
    private Long mid;
    private String menuName;
    private String menuStatus;
    private String menuIcon;

}
