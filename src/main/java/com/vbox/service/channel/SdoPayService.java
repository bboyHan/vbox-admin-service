package com.vbox.service.channel;

import com.vbox.persistent.pojo.dto.SdoWater;
import com.vbox.persistent.pojo.dto.TxWaterList;
import com.vbox.persistent.pojo.param.TxPreAuthParam;

import java.util.List;

public interface SdoPayService {

    boolean tokenCheck(String cookie);

    //2天内的，防止隔天凌晨的问题查单
    List<SdoWater> queryOrderBy2Day(String sessionId, String acid);

    String getCK(String account, String acPwd);
}
