package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.PayOrder;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface POrderMapper extends BaseMapper<PayOrder> {

    @Select("select count(1) from vbox_pay_order where ac_id = #{acId}")
    Integer countPOrderByAcId(String acId);

    @Select("select count(1) from vbox_pay_order where ac_id = #{acId} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderByAcIdToday(String acId);

    @Select("select * from vbox_pay_order where order_id = #{orderId}")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    PayOrder getPOrderByOid(String orderId);

    @Update("update vbox_pay_order set order_status = #{status} where order_id = #{orderId}")
    int updateOStatusByOidForQueue(@Param("orderId") String orderId, @Param("status") int status);

    @Update("update vbox_pay_order set order_status = #{status}, code_use_status = #{useStatus} where order_id = #{orderId}")
    int updateOStatusByOId(@Param("orderId") String orderId, @Param("status") int status, @Param("useStatus") int useStatus);

    //回调成功了
    @Update("update vbox_pay_order set callback_status = 1 where order_id = #{orderId}")
    int updateCallbackStatusByOId(@Param("orderId") String orderId);

    //系统回调
    @Update("update vbox_pay_order set callback_status = 1, call_time = #{callTime} where order_id = #{orderId}")
    int updateCallbackStatusByOIdForSys(@Param("orderId") String orderId, LocalDateTime callTime);

    @Update("update vbox_pay_order set pay_region = #{region} where order_id = #{orderId}")
    int updateRegionByOIdForSys(@Param("orderId") String orderId, String region);

    @Update("update vbox_pay_order set order_status = #{status}, callback_status = 1, code_use_status = #{useStatus} where order_id = #{orderId}")
    int updateStatusByOIdWhenCall(@Param("orderId") String orderId, @Param("status") int status, @Param("useStatus") int useStatus);

    @Update("update vbox_pay_order set order_status = #{status}, callback_status = 0, code_use_status = #{useStatus} where order_id = #{orderId}")
    int updateStatusByOIdWhenCallButFailed(@Param("orderId") String orderId, @Param("status") int status, @Param("useStatus") int useStatus);

    /**
     * 10min 前的单子核对未支付的情况
     */
    @Select("select * from vbox_pay_order where order_status = 2 AND create_time < DATE_SUB(NOW(), INTERVAL 5 MINUTE)")
    List<PayOrder> listUnPay();

    @Update("update vbox_pay_order set order_status = #{status},uid = #{uid},ac_id = #{acid}, code_use_status = 1, resource_url = #{payUrl}, platform_oid = #{platformOid}, pay_ip = #{payIp}, async_time = #{asyncTime} where order_id = #{orderId}")
    int updateInfoForQueue(@Param("orderId") String orderId, @Param("uid") Integer uid, @Param("acid") String acid, @Param("status") int status, @Param("platformOid") String platformOid, @Param("payUrl") String payUrl, @Param("payIp") String payIp, LocalDateTime asyncTime);

    // ===================== 通道 订单数=============================
    // 通道 所有产单
    @Select("select count(1) from vbox_pay_order where p_account = #{pAccount}")
    int countPOrderByPAIDc(String pAccount, String cChannelId);

    // 通道 所有成单
    @Select("select count(1) from vbox_pay_order where p_account = #{pAccount} and order_status = 1")
    int countPOrderPayedByPAIDc(String pAccount, String cChannelId);

    // 通道 昨天产单
    @Select("SELECT count(1) from vbox_pay_order where p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int countPOrderYesterdayByPAIDc(String pAccount, String cChannelId);

    // 通道 昨天成单
    @Select("SELECT count(1) from vbox_pay_order where order_status = 1 and p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int countPOrderPayedYesterdayByPAIDc(String pAccount, String cChannelId);

    // 通道 今天产单
    @Select("select count(1) from vbox_pay_order where p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int countPOrderTodayByPAIDc(String pAccount, String cChannelId);

    // 通道 今天成单
    @Select("select count(1) from vbox_pay_order where order_status = 1 and p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int countPOrderPayedTodayByPAIDc(String pAccount, String cChannelId);

    // =====================通道 订单金额=============================
    // 通道 所有产单金额
    @Select("select sum(cost) from vbox_pay_order where p_account = #{pAccount}")
    int sumPOrderByPAIDc(String pAccount, String cChannelId);

    // 通道 所有成单金额
    @Select("select sum(cost) from vbox_pay_order where p_account = #{pAccount} and order_status = 1")
    int sumPOrderPayedByPAIDc(String pAccount, String cChannelId);

    // 通道 昨天产单金额
    @Select("SELECT sum(cost) from vbox_pay_order where p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int sumPOrderYesterdayByPAIDc(String pAccount, String cChannelId);

    // 通道 昨天成单金额
    @Select("SELECT sum(cost) from vbox_pay_order where p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int sumPOrderPayedYesterdayByPAIDc(String pAccount, String cChannelId);

    // 通道 今天产单金额
    @Select("select sum(cost) from vbox_pay_order where p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int sumPOrderTodayByPAIDc(String pAccount, String cChannelId);

    // 通道 今天成单金额
    @Select("select sum(cost) from vbox_pay_order where order_status = 1 and p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    int sumPOrderPayedTodayByPAIDc(String pAccount, String cChannelId);

//    =========================================================

    // 获取付方对接的通道
    @Select("select distinct c_channel_id from vbox_pay_order where p_account = #{pAccount}")
    List<String> listChannelByPAID(String pAccount);

    // =====================订单数=============================
    // 所有产单
    @Select("select count(1) from vbox_pay_order where p_account = #{pAccount}")
    Integer countPOrderByPAID(String pAccount);

    // 所有成单
    @Select("select count(1) from vbox_pay_order where p_account = #{pAccount} and order_status = 1")
    Integer countPOrderPayedByPAID(String pAccount);

    // 昨天产单
    @Select("SELECT count(1) from vbox_pay_order where p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderYesterdayByPAID(String pAccount);

    // 昨天某通道产单
    @Select("SELECT count(1) from vbox_pay_order where p_account = #{pAccount} AND c_channel_id = #{channel} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderChannelYesterdayByPAID(String pAccount, String channel);

    // 昨天成单
    @Select("SELECT count(1) from vbox_pay_order where order_status = 1 and p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderPayedYesterdayByPAID(String pAccount);

    // 昨天成单某通道
    @Select("SELECT count(1) from vbox_pay_order where order_status = 1 and p_account = #{pAccount} AND c_channel_id = #{channel} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderPayedChannelYesterdayByPAID(String pAccount, String channel);

    // 今天产单
    @Select("select count(1) from vbox_pay_order where p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderTodayByPAID(String pAccount);

    // 今天产单某通道
    @Select("select count(1) from vbox_pay_order where p_account =  #{pAccount} AND c_channel_id = #{channel} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderChannelTodayByPAID(String pAccount, String channel);

    // 今天成单
    @Select("select count(1) from vbox_pay_order where order_status = 1 and p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderPayedTodayByPAID(String pAccount);

    // 今天成单某通道
    @Select("select count(1) from vbox_pay_order where order_status = 1 and p_account =  #{pAccount} AND c_channel_id = #{channel} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer countPOrderPayedChannelTodayByPAID(String pAccount, String channel);

    // =====================订单金额=============================
//    // 所有产单金额
//    @Select("select sum(cost) from vbox_pay_order where p_account = #{pAccount}")
//    int sumPOrderByPAID(String pAccount);

    // 所有成单金额
    @Select("select sum(cost) from vbox_pay_order where p_account = #{pAccount} and order_status = 1")
    Integer sumPOrderPayedByPAID(String pAccount);

//    // 昨天产单金额
//    @Select("SELECT sum(cost) from vbox_pay_order where p_account = #{pAccount} AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
//    int sumPOrderYesterdayByPAID(String pAccount);

    // 昨天成单金额
    @Select("SELECT sum(cost) from vbox_pay_order where p_account = #{pAccount} and order_status = 1 AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer sumPOrderPayedYesterdayByPAID(String pAccount);

    // 昨天成单金额某通道
    @Select("SELECT sum(cost) from vbox_pay_order where p_account = #{pAccount} AND c_channel_id = #{channel} and order_status = 1 AND create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer sumPOrderPayedChannelYesterdayByPAID(String pAccount, String channel);

//    // 今天产单金额
//    @Select("select sum(cost) from vbox_pay_order where p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
//    int sumPOrderTodayByPAID(String pAccount);

    // 今天成单金额
    @Select("select sum(cost) from vbox_pay_order where order_status = 1 and p_account =  #{pAccount} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer sumPOrderPayedTodayByPAID(String pAccount);

    // 今天成单金额某通道
    @Select("select sum(cost) from vbox_pay_order where order_status = 1 and p_account =  #{pAccount} AND c_channel_id = #{channel} AND create_time > DATE_SUB(curdate(),INTERVAL 0 DAY)")
    Integer sumPOrderPayedChannelTodayByPAID(String pAccount, String channel);

    @Select("select count(1) from vbox_pay_order where order_status = 2 and cost = #{money} and ac_id = #{acid}")
    Integer isExistPOrderByAcIdAndStatus(String acid, Integer money);

    @Select("select count(1) from vbox_pay_order where platform_oid like '%' #{acAccount} '%' and cost = #{money} and create_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE)")
    int getPOrderByPre30AndQQ(String acAccount, Integer money);

    @Select("select count(1) from vbox_pay_order where order_status != 1 and platform_oid like '%' #{acAccount} '%' and create_time >= DATE_SUB(NOW(), INTERVAL 8 MINUTE)")
    int getPOrderByPre8AndXoyAcc(String acAccount);

    @Select("select count(1) from vbox_pay_order where platform_oid like '%' #{acAccount} '%'")
    int getPOrderBy2TimeAndSdoInAcc(String acAccount);
}
