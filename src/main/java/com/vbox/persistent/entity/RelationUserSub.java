package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("relation_user_sub")
@Data
public class RelationUserSub {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer uid;
    private Integer sid;

}
