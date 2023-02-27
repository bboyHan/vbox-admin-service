package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.CAEnableParam;
import com.vbox.persistent.pojo.param.CAccountParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface ChannelService {

    VboxUserVO getVboxUser();
    List<Integer> getVboxUserViewOrderSum();
    List<Long> getVboxUserViewOrderNum();

    int createChannelAccount(CAccountParam caParam);

    List<CGatewayVO> getGatewayList(String gateway);

    ResultOfList<List<CAccountVO>> listCAccount(CAccountParam caParam);

    int updateCAccount(CAccountParam param) throws IOException;

    int enableCAccount(CAEnableParam param) throws IOException;

    int deleteCAccount(Integer cid);
}
