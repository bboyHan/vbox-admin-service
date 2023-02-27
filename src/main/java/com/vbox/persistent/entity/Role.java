package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_role")
@Data
public class Role {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderNo;
    private String roleName;
    private String roleValue;
    private LocalDateTime createTime;
    private String status;
    private String remark;
    private String menu;


}
