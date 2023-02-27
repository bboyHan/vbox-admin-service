package com.vbox.persistent.repo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vbox.persistent.entity.CAccountWallet;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CAccountWalletMapper extends BaseMapper<CAccountWallet> {

    @Select("select * from vbox_channel_acwallet where caid = #{caid}")
    List<CAccountWallet> listByCaid(Integer caid);

}
