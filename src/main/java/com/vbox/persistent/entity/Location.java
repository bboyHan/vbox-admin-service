package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("sys_location")
@Data
public class Location {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String area;
    private String region;

}
