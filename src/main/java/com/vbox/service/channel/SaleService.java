package com.vbox.service.channel;

import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.SaleVO;
import com.vbox.persistent.pojo.vo.TotalVO;

import java.util.List;

public interface SaleService {

    List<CAccountVO> listSaleInfo();

    List<CAccountVO> listSaleCAccount();

    int createSub(UserSubCreateOrUpdParam subCreateOrUpdParam) throws Exception;

    List<SaleVO> listSaleOverView();

    TotalVO totalOverView();
}
