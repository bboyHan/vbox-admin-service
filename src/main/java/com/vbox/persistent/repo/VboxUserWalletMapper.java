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
    @Select({"<script>SELECT SUM(recharge) FROM vbox_user_wallet WHERE 1=1<if test='sidList != null and sidList.size()!=0 '> AND uid IN <foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'> #{item}</foreach></if></script>"})
    Integer getTotalRechargeBySidList(List<Integer> sidList);
    @Select({"<script>SELECT SUM(cost) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE 1=1<if test='sidList != null and sidList.size()!=0 '> AND uid IN <foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'> #{item}</foreach></if> )</script>"})
    Integer getTotalCostBySidList(List<Integer> sidList);
    @Select({"<script>SELECT count(1) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE 1=1<if test='sidList != null and sidList.size()!=0 '> AND uid IN <foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'> #{item}</foreach></if> )</script>"})
    Integer getTotalCostNumBySidList(List<Integer> sidList);
    @Select({"<script>SELECT count(1) FROM vbox_pay_order WHERE ac_id IN (SELECT acid FROM vbox_channel_account WHERE 1=1<if test='sidList != null and sidList.size()!=0 '> AND uid IN <foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'> #{item}</foreach></if> )</script>"})
    Integer getTotalNumBySidList(List<Integer> sidList);

    @Select("SELECT SUM(recharge) FROM vbox_user_wallet WHERE uid = #{uid}")
    Integer getTotalRechargeByUid(Integer uid);
    @Select("SELECT SUM(cost) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTotalCostByUid(Integer uid);
    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTotalCostNumByUid(Integer uid);
    // 累计产生总订单量
    @Select("SELECT count(1) FROM vbox_pay_order WHERE ac_id IN (SELECT acid FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTotalProdOrderNum(Integer uid);

    //    总充值 caid ======================================
    @Select("SELECT SUM(cost) FROM vbox_channel_acwallet WHERE caid = #{caid}")
    Integer getTotalCostByCaid(Integer caid);
    //    昨充值 caid ======================================
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid = #{caid}")
    Integer getYesterdayOrderSumByCaid(Integer caid);
    //    前天充值 caid ======================================
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE create_time between DATE_SUB(curdate(),INTERVAL 2 DAY) AND DATE_SUB(curdate(),INTERVAL 1 DAY) AND caid = #{caid}")
    Integer getBeforeDayOrderSumByCaid(Integer id);
    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getYesterdayOrderNum(Integer uid);
    @Select("SELECT count(1) FROM vbox_pay_order WHERE create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY) AND ac_id IN (SELECT acid FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getYesterdayProdOrderNum(Integer uid);
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE create_time between DATE_SUB(curdate(),INTERVAL 1 DAY) AND DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getYesterdayOrderSum(Integer uid);

    //    今充值 caid ======================================
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE create_time > DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid = #{caid}")
    Integer getTodayOrderSumByCaid(Integer caid);
    @Select("SELECT * FROM vbox_channel_acwallet WHERE create_time > DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    List<CAccountWallet> getTodayOrder(Integer uid);
    // 过去24小时
    @Select("SELECT * FROM vbox_channel_acwallet WHERE TO_DAYS(NOW()) - TO_DAYS(create_time) <= 1 AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    List<CAccountWallet> getLast24HOrder(Integer uid);
    @Select("SELECT count(1) FROM vbox_channel_acwallet WHERE create_time > DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTodayOrderNum(Integer uid);
    // 今充单量
    @Select("SELECT sum(cost) FROM vbox_channel_acwallet WHERE create_time > DATE_SUB(curdate(),INTERVAL 0 DAY) AND caid IN (SELECT id FROM vbox_channel_account WHERE uid = #{uid})")
    Integer getTodayOrderSum(Integer uid);

    // 今产单量
    @Select("SELECT count(1) FROM vbox_pay_order WHERE create_time > DATE_SUB(curdate(),INTERVAL 0 DAY) AND ac_id IN (SELECT acid FROM vbox_channel_account WHERE uid = #{uid})")
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


    /*
     * ==========================================
     */
    @Select("<script>" +
            "SELECT *  FROM vbox_user_wallet WHERE 1=1" +
            "<if test='sidList != null and sidList.size()!=0 '>" +
            " AND uid IN " +
            "<foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</if>" +
            " order by id desc limit #{page}, #{pageSize}" +
            "</script>")
    List<VboxUserWallet> listSubUserWallet(List<Integer> sidList, Integer page, Integer pageSize);

    @Select("<script>" +
            "SELECT count(1) FROM vbox_user_wallet WHERE 1=1" +
            "<if test='sidList != null and sidList.size()!=0 '>" +
            " AND uid IN " +
            "<foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</if>" +
            "</script>")
    Integer countSubUserWallet(List<Integer> sidList);

}
