package com.vbox.service.channel;

import com.vbox.persistent.pojo.dto.TxWaterList;
import com.vbox.persistent.pojo.param.TxPreAuthParam;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TxPayService {

    String preAuth(TxPreAuthParam authParam);

    boolean tokenCheck(String openId, String openKey);

    List<TxWaterList> queryOrderBy30(String openId, String openKey);

    List<TxWaterList> queryOrderTXACBy30(String acAccount);
}
