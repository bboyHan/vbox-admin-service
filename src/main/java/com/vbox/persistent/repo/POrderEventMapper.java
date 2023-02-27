package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.PayOrderEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface POrderEventMapper extends BaseMapper<PayOrderEvent> {

    @Select("select * from vbox_pay_order_event where order_id = #{orderId}")
    PayOrderEvent getPOrderEventByOid(String orderId);

}
