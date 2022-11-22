package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("relation_user_role")
@Data
public class RelationUserRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uid;
    private Long rid;

}
