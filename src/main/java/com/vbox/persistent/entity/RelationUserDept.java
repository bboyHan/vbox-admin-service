package com.vbox.persistent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("relation_user_dept")
@Data
public class RelationUserDept {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long uid;
    private Long did;

}
