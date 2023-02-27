package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.PAccount;
import com.vbox.persistent.pojo.vo.PAccountVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PAccountMapper extends BaseMapper<PAccount> {

    @Results(
            value = {
                    @Result(property = "p_account", column = "p_account"),
                    @Result(property = "p_key", column = "p_key"),
                    @Result(property = "p_remark", column = "p_remark"),
                    @Result(property = "create_time", column = "create_time"),
            }
    )
    @Select("select p.id, p.p_account as p_account,p.p_key as p_key, p.p_remark as p_remark, a.pub, a.secret, p.status, p.create_time as create_time" +
            " from vbox_pay_account p left join vbox_pay_auth a on p.id = a.pid")
    List<PAccountVO> listPAccountInfo();

    @Results(
            value = {
                    @Result(property = "p_account", column = "p_account"),
                    @Result(property = "p_key", column = "p_key"),
                    @Result(property = "p_remark", column = "p_remark"),
                    @Result(property = "create_time", column = "create_time"),
            }
    )
    @Select("select p.id, p.p_account as p_account, p.p_key as p_key , p.p_remark as p_remark, a.pub, a.secret, p.status, p.create_time as create_time" +
            " from vbox_pay_account p left join vbox_pay_auth a on p.id = a.pid" +
            " where p.p_account = #{pAccount} and a.pub = #{pKey}")
    PAccountVO getInfoByAccountAndPub(String pAccount, String pKey);
}
