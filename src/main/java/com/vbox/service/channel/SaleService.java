package com.vbox.service.channel;

import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;

import java.util.List;

public interface SaleService {

    List listSaleInfo();

    List listSaleCAccount();

    int createSub(UserSubCreateOrUpdParam subCreateOrUpdParam) throws Exception;

}
