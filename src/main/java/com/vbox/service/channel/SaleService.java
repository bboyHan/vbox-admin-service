package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.SaleVO;
import com.vbox.persistent.pojo.vo.TotalVO;

import java.util.List;

public interface SaleService {

    List<SaleVO> listSaleInfo();

    ResultOfList listSaleCAccount(Integer status,Integer page, Integer pageSize);

    int createSub(UserSubCreateOrUpdParam subCreateOrUpdParam) throws Exception;

    List<SaleVO> listSaleOverView();

    TotalVO totalOverView();
}
