package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.PayOrderEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface POrderEventMapper extends BaseMapper<PayOrderEvent> {

    @Select("select * from vbox_pay_order_event where order_id = #{orderId}")
    PayOrderEvent getPOrderEventByOid(String orderId);

    @Update("update vbox_pay_order_event set event_log = #{eventLog}, ext = #{ext}, platform_oid = #{platformOid} where order_id = #{orderId}")
    int updateInfoForQueue(@Param("orderId") String orderId, @Param("eventLog") String eventLog, @Param("platformOid") String platformOid, @Param("ext") String ext);
}
