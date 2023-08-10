package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.entity.ChannelShop;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.param.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public interface ChannelPreService {

    int batchChannelPre(MultipartFile multipartFile);

    List<ChannelPreCount> countForCAccounts(ChannelPreParam csParam);

    List<CAccount> listCAccount(ChannelPreParam param);

    int createChannelPre(ChannelPreParam csParam) throws Exception;

    ResultOfList<List<ChannelPre>> listChannelPre(ChannelPreParam csParam);

    int updateChannelPre(ChannelPreParam param);

    int deleteChannelPre(Integer id);

    List<CChannel> getChannelPreTypes(ChannelPreParam channelShopParam);

    int batchCreateChannelPre(ChannelPreBatchParam param);

    int batchCreateChannelPreForAcList(ChannelPreBatchAcListParam param);

    int clearChannelPre(String acid);
}
