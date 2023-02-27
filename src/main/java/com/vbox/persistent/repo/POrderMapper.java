package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.PayOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface POrderMapper extends BaseMapper<PayOrder> {

    @Select("select count(1) from vbox_pay_order where ac_id = #{acId}")
    Integer countPOrderByAcId(String acId);

    @Select("select * from vbox_pay_order where order_id = #{orderId}")
    PayOrder getPOrderByOid(String orderId);

    PayOrder updatePOrderByOid(String orderId);

    @Update("update vbox_pay_order set order_status = #{status}, code_use_status = #{useStatus} where order_id = #{orderId}")
    int updateOStatusByOId(@Param("orderId") String orderId, @Param("status") int status, @Param("useStatus") int useStatus);

    //回调成功了
    @Update("update vbox_pay_order set callback_status = 1 where order_id = #{orderId}")
    int updateCallbackStatusByOId(@Param("orderId") String orderId);


    @Update("update vbox_pay_order set order_status = #{status}, callback_status = 1, code_use_status = #{useStatus} where order_id = #{orderId}")
    int updateStatusByOIdWhenCall(@Param("orderId") String orderId, @Param("status") int status, @Param("useStatus") int useStatus);

    @Update("update vbox_pay_order set order_status = #{status}, callback_status = 0, code_use_status = #{useStatus} where order_id = #{orderId}")
    int updateStatusByOIdWhenCallButFailed(@Param("orderId") String orderId, @Param("status") int status, @Param("useStatus") int useStatus);

    @Select("select * from vbox_pay_order where order_status = 2")
    List<PayOrder> listUnPay();
}
