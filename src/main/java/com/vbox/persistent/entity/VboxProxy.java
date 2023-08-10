package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("vbox_proxy")
@Data
public class VboxProxy {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String chan;
    private String url;
    private Integer type;

}
