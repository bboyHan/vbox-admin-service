package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.pojo.dto.CAccountInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CAccountMapper extends BaseMapper<CAccount> {
    @Select({"<script>SELECT * FROM vbox_channel_account WHERE 1=1<if test='sidList != null and sidList.size()!=0 '> AND uid IN <foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'> #{item}</foreach></if></script>"})
    List<CAccount> listSaleInUids(@Param("sidList") List<Integer> sidList);

    @Select("<script>" +
            "SELECT * FROM vbox_channel_account WHERE 1=1" +
            "<if test='sidList != null and sidList.size()!=0 '>" +
            " AND uid IN " +
            "<foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</if>" +
            " order by id desc" +
            "</script>")
    List<CAccount> listACInUids(@Param("sidList") List<Integer> sidList);

    @Select({"select count(1) from vbox_channel_account where uid = #{uid}"})
    Integer countByUid(Integer uid);
    @Select({"select count(1) from vbox_channel_account where uid = #{uid} and status = 1"})
    Integer countACEnableByUid(Integer uid);

    @Select("<script>" +
            "SELECT acid FROM vbox_channel_account WHERE 1=1" +
            "<if test='sidList != null and sidList.size()!=0 '>" +
            " AND uid IN " +
            "<foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</if>"
            + "</script>")
    List<String> listAcIdInUids(@Param("sidList") List<Integer> sidList);


    @Select("select a.*, w.id as wid, w.caid, w.cost, w.oid, w.create_time from vbox_channel_account a left JOIN vbox_channel_acwallet w" +
            " ON a.id = w.caid where a.status = 1 and a.sys_status = 1")
    @Results(id = "listCAccount",value = {
            @Result(id = true,column = "id",property = "id"),
            @Result(column = "caid",property = "caid"),
            @Result(column = "cost",property = "cost"),
            @Result(column = "oid",property = "oid"),
            @Result(column = "create_time",property = "createTime"),
    })
    List<CAccountInfo> listCanPayForCAccount();


    @Select("select a.*, w.id as wid, w.caid, w.cost, w.oid, w.create_time from vbox_channel_account a left JOIN vbox_channel_acwallet w" +
            " ON a.id = w.caid AND w.create_time > #{createTime} where a.status = 1 and a.sys_status = 1")
    @Results(id = "listCAccountToday",value = {
            @Result(id = true,column = "id",property = "id"),
            @Result(column = "caid",property = "caid"),
            @Result(column = "cost",property = "cost"),
            @Result(column = "oid",property = "oid"),
            @Result(column = "create_time",property = "createTime"),
    })
    List<CAccountInfo> listCanPayForCAccountToday(String createTime);

    @Select("select * from vbox_channel_account where acid = #{acid}")
    CAccount getCAccountByAcid(String acId);

    @Update("update vbox_channel_account set sys_status = 0, sys_log = #{sysLog} where uid = #{uid}")
    void stopByUid(@Param("sysLog")String sysLog, @Param("uid") Integer uid);
    @Update("update vbox_channel_account set sys_status = 1, sys_log = #{sysLog} where uid = #{uid}")
    void startByUid(@Param("sysLog")String sysLog, @Param("uid") Integer uid);
}
