package com.vbox.service.channel;

import com.vbox.persistent.pojo.param.TxPreAuthParam;

public interface TxPayService {

    String preAuth(TxPreAuthParam authParam);
}
