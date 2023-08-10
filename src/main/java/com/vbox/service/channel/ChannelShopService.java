package com.vbox.service.channel;

import com.vbox.common.ResultOfList;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.Channel;
import com.vbox.persistent.entity.ChannelShop;
import com.vbox.persistent.pojo.dto.ChannelMultiShop;
import com.vbox.persistent.pojo.dto.ChannelMultiTreeShop;
import com.vbox.persistent.pojo.param.CSEnableParam;
import com.vbox.persistent.pojo.param.ChannelShopParam;

import java.util.List;

public interface ChannelShopService {

    int createChannelShop(ChannelShopParam csParam);

    ResultOfList<List<ChannelShop>> listChannelShop(ChannelShopParam csParam);

    int updateChannelShop(ChannelShopParam param);

    int enableChannelShop(CSEnableParam param);

    int deleteChannelShop(Integer id);

    List<CChannel> getChannelShopTypes(ChannelShopParam channelShopParam);

    ResultOfList<List<ChannelShop>> listManageChannelShop(String shopRemark);

    ResultOfList<List<ChannelMultiShop>> listMultiChannelShop(ChannelShopParam ChannelShopParam);

    int multiEnableChannelShop(String shopRemark, Integer status);

    int updateShopAddress(String address, Integer id);

    int deleteChannelShopByShopRemark(String shopRemark);

    ResultOfList<List<ChannelMultiTreeShop>> listMultiTreeChannelShop(ChannelShopParam channelShopParam);
}
