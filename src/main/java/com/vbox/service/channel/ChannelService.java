package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.CAEnableParam;
import com.vbox.persistent.pojo.param.CAccountParam;
import com.vbox.persistent.pojo.param.TxCAccountParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface ChannelService {

    VboxUserVO getVboxUser();
    List<Object> getVboxUserViewOrderSum();
    List<Long> getVboxUserViewOrderNum();

    int createChannelAccount(CAccountParam caParam);
    int createSdoChannelAccount(CAccountParam caParam);

    int createTxChannelAccount(TxCAccountParam caParam);

    List<CGatewayVO> getGatewayList(String gateway);

    ResultOfList<List<CAccountVO>> listCAccount(CAccountParam caParam);

    int updateCAccount(CAccountParam param) throws IOException;
    int updateTxCAccount(TxCAccountParam param) throws IOException;
    int updateSdoCAccount(CAccountParam param) throws IOException;

    int enableCAccount(CAEnableParam param) throws IOException;

    int deleteCAccount(Integer cid);

    String getTxQuery(String orderId);

}
