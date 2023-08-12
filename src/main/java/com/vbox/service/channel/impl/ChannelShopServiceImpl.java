package com.vbox.service.channel.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vbox.common.ResultOfList;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelShop;
import com.vbox.persistent.pojo.dto.ChannelMultiShop;
import com.vbox.persistent.pojo.dto.ChannelMultiTreeShop;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public int batchCreateChannelShop(JSONObject data) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        String channel = data.getString("channel");
        String shopRemark = data.getString("shopRemark");

        CChannel channelDB = channelMapper.getChannelByChannelId(channel);
        String addrKey = "address";
        int cnt = 0;

        for (String key : data.keySet()) {

            Integer money = 0;
            String address = "";

            if (key.matches("^money-\\d+$")) {
                ChannelShop channelShop = new ChannelShop();
                // 提取索引
                String index = key.substring(6);
                money = Integer.parseInt(data.getString(key));
                address = data.getString(addrKey + "-" + index);
                channelShop.setMoney(money);
                channelShop.setAddress(address);
                channelShop.setShopRemark(shopRemark);
                channelShop.setUid(uid);
                channelShop.setChannel(channelDB.getCChannelId());
                channelShop.setCid(channelDB.getId());
                channelShop.setCreateTime(LocalDateTime.now());
                int row = channelShopMapper.insert(channelShop);
                cnt += row;
            }

        }

        return cnt;
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
//        System.out.println(uid);
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
        List<ChannelShop> records = channelShopMapper.queryByUid(uid);
//        System.out.println(records.toString());
        List<ChannelMultiShop> result = getMultiList(records);
        ResultOfList rs = new ResultOfList(result, result.size());
        return rs;
    }

    private List<ChannelMultiShop> getMultiList(List<ChannelShop> records){
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
                    int  open = 0;
                    int  total = 0;
                    String openAndClose = "已启用【%s】个 , 共【%s】个";
                    for (int i = 0; i < shops.size(); i++) {
                        if (shops.get(i).getStatus() == 1){
                            open ++;
                        }
                        total ++;
                    }
                    openAndClose = String.format(openAndClose, open, total);

                    int status = 0;
                    if (open > 0){
                        status = 1;
                    }
                    return new ChannelMultiShop(shops.get(0).getUid(),
                            status,
                            shops.get(0).getChannel(),
                            mark,
                            money,
                            openAndClose);
                })
                .collect(Collectors.toList());

        return result;
    }

    @Override
    public ResultOfList<List<ChannelMultiTreeShop>> listMultiTreeChannelShop(ChannelShopParam channelShopParam) {
        List<ChannelMultiTreeShop> list = new ArrayList<>();

        Integer uid = TokenInfoThreadHolder.getToken().getId();
//        log.info("uid:{}", uid);
        List<ChannelShop> records = channelShopMapper.queryByUid(uid);
//        log.info("records:{}", records);
        List<ChannelMultiShop> result = getMultiList(records);
//        log.info("result:{}", result);

        Map<String, List<ChannelShop>> groupedRecords = records.stream()
                .collect(Collectors.groupingBy(ChannelShop::getChannel));
        List<ChannelShop> filteredRecords = new ArrayList<>();
        for (List<ChannelShop> group : groupedRecords.values()) {
            Optional<ChannelShop> minRecord = group.stream()
                    .min(Comparator.comparing(ChannelShop::getCreateTime));
            minRecord.ifPresent(filteredRecords::add);
        }
//        log.info("filteredRecords:{}", filteredRecords);
        for (int i = 0; i < filteredRecords.size(); i++) {
            ChannelMultiTreeShop treeShop = new ChannelMultiTreeShop();
            String channel = filteredRecords.get(i).getChannel();
            System.out.println(channel);
            List<ChannelMultiShop> filteredList = result.stream()
                    .filter(item -> item.getChannel().equals(channel))
                    .collect(Collectors.toList());
            log.info("filteredList:{}", filteredList);
            if (filteredList.size() > 0){
                ChannelMultiShop multiShop = filteredList.get(0);
                treeShop.setId(i + 1 + "");
                treeShop.setUid(uid);
                treeShop.setStatus(multiShop.getStatus());
                treeShop.setChannel(channel);
                treeShop.setShopRemark(multiShop.getShopRemark());
                treeShop.setMoney(multiShop.getMoney());
                treeShop.setOpenAndClose(multiShop.getOpenAndClose());
                if (filteredList.size() > 1){
                    List<ChannelMultiShop> childrenShops = IntStream.range(1, filteredList.size())
                            .mapToObj(filteredList::get)
                            .collect(Collectors.toList());
                    treeShop.setChildren(childrenShops);
                }
            }

            list.add(treeShop);

        }
        ResultOfList rs = new ResultOfList(list, list.size());

        return rs;
    }


    @Override
    public int multiEnableChannelShop(String shopRemark, Integer status) {
        List<ChannelShop>  list = channelShopMapper.getChannelShopListByMark(shopRemark);
        int rows = 0;
        for (ChannelShop shop : list) {
            shop.setStatus(status);
            int row = channelShopMapper.updateById(shop);
            if(row > 0){
                rows ++;
            }
        }

        return rows;
    }

    @Override
    public int updateShopAddress(String address, Integer id) {
        ChannelShop channelShop = new ChannelShop();
        channelShop.setId(id);
        channelShop.setAddress(address);
        int row = channelShopMapper.updateById(channelShop);
        return row;
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
    public int deleteChannelShopByShopRemark(String shopRemark) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        int row = channelShopMapper.deleteByMark(uid, shopRemark);
        return row;
    }


    @Override
    public List<CChannel> getChannelShopTypes(ChannelShopParam channelShopParam) {
        List<CChannel> channels = channelMapper.getChannelShopTypes();
        return channels;
    }




}
