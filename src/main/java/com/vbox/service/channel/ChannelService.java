package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;
import org.springframework.web.multipart.MultipartFile;

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
    int updateXoyCAccount(CAccountParam param) throws IOException;

    int enableCAccount(CAEnableParam param) throws IOException;

    int enableBatchCAccount(List<String> acidList, Integer status) throws IOException ;

    int deleteCAccount(Integer cid);

    Object getTxQuery(String orderId);

    Object getAccQuery(String acid);

    int batchChannelAccount(MultipartFile multipartFile);

    int deleteBatchCAccount(List<String> acidList);

    List<CAccount> listAllCAccount(CAccountParam param);

    int enableCAccountList(ChannelBatchAcListParam param);

}
