package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.entity.VboxUserWallet;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.MngSaleVO;
import com.vbox.persistent.pojo.vo.SaleVO;
import com.vbox.persistent.pojo.vo.TotalVO;

import java.util.List;

public interface SaleService {

    List<SaleVO> listSaleInfo();
    List<User> listSaleUser();
    ResultOfList listSaleRecharge(Integer page, Integer pageSize);

    ResultOfList listSaleCAccount(Integer status, String saleName, String acRemark,Integer page, Integer pageSize);

    int createSub(UserSubCreateOrUpdParam subCreateOrUpdParam) throws Exception;

    List<SaleVO> listSaleOverView();

    TotalVO totalOverView();

    ResultOfList listSaleCAOverviewToday();

    MngSaleVO getMngSaleRecharge();

    int mngSaleTransferSub(Integer recharge);

    Object addSaleRecharge(Integer uid, Integer recharge);
}
