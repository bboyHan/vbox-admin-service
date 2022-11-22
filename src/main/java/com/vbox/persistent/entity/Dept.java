package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_dept")
@Data
public class Dept {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pid;
    private Long orderNo;
    private String deptName;
    private LocalDateTime createTime;
    private String remark;

}
