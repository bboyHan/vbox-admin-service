package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("relation_role_menu")
@Data
public class RelationRoleMenu {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer rid;
    private Integer mid;

}
