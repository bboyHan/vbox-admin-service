package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.entity.ChannelShop;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.param.CSEnableParam;
import com.vbox.persistent.pojo.param.ChannelPreParam;
import com.vbox.persistent.pojo.param.ChannelShopParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChannelPreService {

    int batchChannelPre(MultipartFile multipartFile);

    List<ChannelPreCount> countForCAccounts(ChannelPreParam csParam);

    List<CAccount> listCAccount();

    int createChannelPre(ChannelPreParam csParam);

    ResultOfList<List<ChannelPre>> listChannelPre(ChannelPreParam csParam);

    int updateChannelPre(ChannelPreParam param);

    int deleteChannelPre(Integer id);

    List<CChannel> getChannelPreTypes(ChannelPreParam channelShopParam);
}
