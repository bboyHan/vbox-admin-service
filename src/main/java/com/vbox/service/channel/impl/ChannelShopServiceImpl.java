package com.vbox.service.channel.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vbox.common.ResultOfList;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelShop;
import com.vbox.persistent.pojo.dto.ChannelMultiShop;
import com.vbox.persistent.pojo.param.CSEnableParam;
import com.vbox.persistent.pojo.param.ChannelShopParam;
import com.vbox.persistent.repo.ChannelMapper;
import com.vbox.persistent.repo.ChannelShopMapper;
import com.vbox.service.channel.ChannelShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChannelShopServiceImpl implements ChannelShopService {

    @Autowired
    private ChannelShopMapper channelShopMapper;
    @Autowired
    private ChannelMapper channelMapper;

    @Override
    public int createChannelShop(ChannelShopParam csParam) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();

        CChannel channelDB = channelMapper.getChannelByChannelId(csParam.getChannel());

        ChannelShop channelShop = new ChannelShop();
        channelShop.setShopRemark(csParam.getShopRemark());
        channelShop.setMoney(csParam.getMoney());
        channelShop.setAddress(csParam.getAddress());
        channelShop.setUid(uid);
        channelShop.setChannel(channelDB.getCChannelId());
        channelShop.setCid(channelDB.getId());
        channelShop.setCreateTime(LocalDateTime.now());
        int row = channelShopMapper.insert(channelShop);

        return row;
    }

    @Override
    public ResultOfList<List<ChannelShop>> listChannelShop(ChannelShopParam queryParam) {

        Integer uid = TokenInfoThreadHolder.getToken().getId();
        QueryWrapper<ChannelShop> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(queryParam.getShopRemark())) {
            queryWrapper.eq("shop_remark", queryParam.getShopRemark());
        }
        if (StringUtils.hasLength(queryParam.getChannel())) {
            queryWrapper.likeRight("channel", queryParam.getChannel());
        }

        if (queryParam.getStatus() != null) {
            queryWrapper.eq("status", queryParam.getStatus());
        }

        if (queryParam.getMoney() != null) {
            queryWrapper.eq("money", queryParam.getMoney());
        }

        queryWrapper.eq("uid", uid);
        queryWrapper.orderByDesc("shop_remark");

        Page<ChannelShop> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 20);
        }

        Page<ChannelShop> csPage = channelShopMapper.selectPage(page, queryWrapper);
        List<ChannelShop> records = csPage.getRecords();

        ResultOfList rs = new ResultOfList(records, (int) page.getTotal());
        return rs;
    }


    @Override
    public ResultOfList<List<ChannelShop>> listManageChannelShop(String shopRemark) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        System.out.println(uid);
        QueryWrapper<ChannelShop> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(shopRemark)) {
            queryWrapper.eq("shop_remark", shopRemark);
        }
        queryWrapper.eq("uid", uid);
        Page<ChannelShop> page = new Page<>(1, 20);

        Page<ChannelShop> csPage = channelShopMapper.selectPage(page, queryWrapper);
        List<ChannelShop> records = csPage.getRecords();

        ResultOfList rs = new ResultOfList(records, (int) page.getTotal());
        return rs;
    }

    @Override
    public ResultOfList<List<ChannelMultiShop>> listMultiChannelShop(ChannelShopParam queryParam) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        QueryWrapper<ChannelShop> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(queryParam.getShopRemark())) {
            queryWrapper.eq("shop_remark", queryParam.getShopRemark());
        }
        if (StringUtils.hasLength(queryParam.getChannel())) {
            queryWrapper.likeRight("channel", queryParam.getChannel());
        }

        if (queryParam.getStatus() != null) {
            queryWrapper.eq("status", queryParam.getStatus());
        }

        if (queryParam.getMoney() != null) {
            queryWrapper.eq("money", queryParam.getMoney());
        }

        queryWrapper.eq("uid", uid);
        queryWrapper.orderByDesc("shop_remark");

        Page<ChannelShop> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 20);
        }


        Page<ChannelShop> csPage = channelShopMapper.selectPage(page, queryWrapper);
        List<ChannelShop> records = csPage.getRecords();
        List<ChannelMultiShop> result = records.stream()
                .collect(Collectors.groupingBy(ChannelShop::getShopRemark))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<ChannelShop> shops = entry.getValue();
                    String mark = entry.getKey();
                    String money = shops.stream()
                            .map(ChannelShop::getMoney)
                            .distinct()
                            .sorted()
                            .map(String::valueOf)
                            .collect(Collectors.joining("-"));
                    return new ChannelMultiShop(shops.get(0).getUid(), shops.get(0).getChannel(), mark, money);
                })
                .collect(Collectors.toList());
        ResultOfList rs = new ResultOfList(result, (int) page.getTotal());
        return rs;
    }

    @Override
    public int updateChannelShop(ChannelShopParam param) {
        ChannelShop channelShop = new ChannelShop();
        channelShop.setId(param.getId());
        channelShop.setAddress(param.getAddress());
        int row = channelShopMapper.updateById(channelShop);
        return row;
    }

    @Override
    public int enableChannelShop(CSEnableParam param) {
        ChannelShop channelShop = new ChannelShop();
        channelShop.setId(param.getId());
        channelShop.setStatus(param.getStatus());
        int row = channelShopMapper.updateById(channelShop);
        return row;
    }

    @Override
    public int deleteChannelShop(Integer id) {
        int row = channelShopMapper.deleteById(id);
        return row;
    }

    @Override
    public List<CChannel> getChannelShopTypes(ChannelShopParam channelShopParam) {
        List<CChannel> channels = channelMapper.getChannelShopTypes();
        return channels;
    }


}
