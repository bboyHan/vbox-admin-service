package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.CAccountWallet;
import com.vbox.persistent.entity.VboxUserWallet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface VboxUserWalletMapper extends BaseMapper<VboxUserWallet> {

    @Select("SELECT SUM(recharge) FROM vbox_user_wallet WHERE uid = #{uid}")
    Integer getTotalRechargeByUid(Integer uid);

    @Select("SELECT SUM(cost) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTotalCostByUid(Integer uid);
    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTotalCostNumByUid(Integer uid);

    @Select("SELECT SUM(cost) FROM vbox_channel_acwallet WHERE caid = #{caid}")
    Integer getTotalCostByCaid(Integer caid);
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 0 AND caid = #{caid}")
    Integer getTodayOrderSumByCaid(Integer caid);

    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 1 AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getYesterdayOrderNum(Integer uid);
    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 0 AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTodayOrderNum(Integer uid);
    @Select("SELECT * FROM vbox_channel_acwallet WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 0 AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    List<CAccountWallet> getTodayOrder(Integer uid);
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 1 AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getYesterdayOrderSum(Integer uid);
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 0 AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTodayOrderSum(Integer uid);


    // 累计产生总订单量
    @Select("SELECT count(1) FROM vbox_pay_order WHERE ac_id IN (SELECT acid FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTotalProdOrderNum(Integer uid);

    @Select("SELECT count(1) FROM vbox_pay_order WHERE TO_DAYS(NOW())-TO_DAYS(create_time) = 0 AND ac_id IN (SELECT acid FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTodayProdOrderNum(Integer uid);

    // 1hour 总产生订单量
    @Select("SELECT count(1) FROM vbox_pay_order WHERE ac_id IN (SELECT acid FROM vbox_channel_account WHERE uid = #{uid}) AND create_time > DATE_SUB(NOW(), INTERVAL 60 MINUTE)")
    Integer getHourProdOrderNum(Integer uid);

    // 1hour 成功订单量
    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid}) AND create_time > DATE_SUB(NOW(), INTERVAL 60 MINUTE)")
    Integer getHourOrderNum(Integer uid);

    // 1hour 成功充值金额
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid}) AND create_time > DATE_SUB(NOW(), INTERVAL 60 MINUTE)")
    Integer getHourOrderSum(Integer uid);

}
