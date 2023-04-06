package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.pojo.dto.CAccountInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

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
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            "<if test='acRemark != null'>" +
            " AND ac_remark like concat('%',#{acRemark},'%')" +
            "</if>" +
            " order by id desc limit #{page}, #{pageSize}" +
            "</script>")
    List<CAccount> listACInUids(@Param("sidList") List<Integer> sidList, String acRemark, Integer status, Integer page,  Integer pageSize);

    @Select("<script>" +
            "SELECT count(1) FROM vbox_channel_account WHERE 1=1" +
            "<if test='sidList != null and sidList.size()!=0 '>" +
            " AND uid IN " +
            "<foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            " order by id desc" +
            "</script>")
    Integer countACInUids(@Param("sidList") List<Integer> sidList, Integer status, Integer page, Integer pageSize);

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

    @Select("SELECT * FROM vbox_channel_account WHERE uid = #{uid}")
    List<CAccount> listAcInUid(Integer uid);

    @Select("SELECT * FROM vbox_channel_account WHERE uid = #{uid}")
    Set<String> setAcIdInUid(Integer uid);

    @Select("select a.*, w.id as wid, w.caid, w.cost, w.oid, w.create_time from vbox_channel_account a left JOIN vbox_channel_acwallet w" +
            " ON a.id = w.caid where a.status = 1 and a.sys_status = 1")
    @Results(id = "listCAccount",value = {
            @Result(id = true,column = "id",property = "id"),
            @Result(column = "caid",property = "caid"),
            @Result(column = "cost",property = "cost"),
            @Result(column = "oid",property = "oid"),
            @Result(column = "create_time",property = "createTime"),
    })
    @Options(flushCache = Options.FlushCachePolicy.FALSE ,timeout = 300000)
    List<CAccountInfo> listCanPayForCAccount();


    @Select("select a.*, w.id as wid, w.caid, w.cost, w.oid, w.create_time from vbox_channel_account a left JOIN vbox_channel_acwallet w" +
            " ON a.id = w.caid AND w.create_time > DATE_SUB(curdate(),INTERVAL 0 DAY) where a.status = 1 and a.sys_status = 1")
    @Results(id = "listCAccountToday",value = {
            @Result(id = true,column = "id",property = "id"),
            @Result(column = "caid",property = "caid"),
            @Result(column = "cost",property = "cost"),
            @Result(column = "oid",property = "oid"),
            @Result(column = "create_time",property = "createTime"),
    })
    @Options(flushCache = Options.FlushCachePolicy.FALSE ,timeout = 300000)
    List<CAccountInfo> listCanPayForCAccountToday(String createTime);

    @Select("select * from vbox_channel_account where acid = #{acid}")
    CAccount getCAccountByAcid(String acId);

    @Update("update vbox_channel_account set sys_status = 0, sys_log = #{sysLog} where uid = #{uid}")
    void stopByUid(@Param("sysLog")String sysLog, @Param("uid") Integer uid);

    @Update("update vbox_channel_account set status = 0, sys_status = 0, sys_log = #{sysLog} where id = #{id}")
    void stopByCaId(@Param("sysLog")String sysLog, @Param("id") Integer id);

    @Update("update vbox_channel_account set sys_status = 1, sys_log = #{sysLog} where uid = #{uid}")
    void startByUid(@Param("sysLog")String sysLog, @Param("uid") Integer uid);
}
