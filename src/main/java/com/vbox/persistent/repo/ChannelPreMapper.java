package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChannelPreMapper extends BaseMapper<ChannelPre> {

    @Update("update vbox_channel_pre_code set status = #{status} where plat_oid = #{platformOid}")
    int updateByPlatId(String platformOid, Integer status);

    @Select("select * from vbox_channel_pre_code where ckid = #{ckid}")
    List<ChannelPre> listChannelPreByCKID(String ckid);

    @Select("select address from vbox_channel_pre_code where plat_oid = #{platformOid}")
    String getAddressByPlatOid(String platformOid);

    @Select("select ckid from vbox_channel_pre_code where plat_oid = #{platformOid}")
    String getCKIDbyPlatOid(String platformOid);

    @Select("<script>" +
            "SELECT COUNT(1) as count, acid, ac_account as acAccount, status FROM vbox_channel_pre_code WHERE 1=1" +
            "<if test='sidList != null and sidList.size()!=0 '>" +
            " AND uid IN " +
            "<foreach item='item' index='index' collection='sidList' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            "<if test='acAccount != null'>" +
            " AND ac_account like concat('%',#{acAccount},'%')" +
            "</if>" +
            " GROUP BY acid,status" +
            "</script>")
    List<ChannelPreCount> countForCAccounts(@Param("sidList") List<Integer> sidList, Integer status, String acAccount);


}
