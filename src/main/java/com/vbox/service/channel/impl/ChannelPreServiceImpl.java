package com.vbox.service.channel.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vbox.common.ResultOfList;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.entity.ChannelPre;
import com.vbox.persistent.pojo.dto.ChannelPreCount;
import com.vbox.persistent.pojo.dto.SdoWater;
import com.vbox.persistent.pojo.param.ChannelPreParam;
import com.vbox.persistent.repo.CAccountMapper;
import com.vbox.persistent.repo.ChannelMapper;
import com.vbox.persistent.repo.ChannelPreMapper;
import com.vbox.persistent.repo.RelationUSMapper;
import com.vbox.service.channel.ChannelPreService;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.SdoPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChannelPreServiceImpl implements ChannelPreService {

    @Autowired
    private ChannelPreMapper channelPreMapper;
    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private CAccountMapper cAccountMapper;
    @Autowired
    private RelationUSMapper relationUSMapper;
    @Autowired
    private PayService payService;
    @Autowired
    private SdoPayService sdoPayService;

    public Long unUsedCount() {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        QueryWrapper<ChannelPre> unUsedQueryWrapper = new QueryWrapper<>();
        unUsedQueryWrapper.eq("status", 2);
        Long count = channelPreMapper.selectCount(unUsedQueryWrapper);
        return count;
    }

    @Override
    public List<ChannelPreCount> countForCAccounts(ChannelPreParam csParam) {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        List<ChannelPreCount> countList = channelPreMapper.countForCAccounts(sidList, 2, csParam.getAcAccount());
        return countList;
    }

    @Override
    public List<CAccount> listCAccount() {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        List<CAccount> caList = cAccountMapper.listACInUids(sidList, null, 1, 0, 999);
//        Integer count = cAccountMapper.countACInUids(sidList, 1, 0, 999);

        return caList;
    }

    @Override
    public int createChannelPre(ChannelPreParam csParam) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();

        CChannel channelDB = channelMapper.getChannelByChannelId(csParam.getChannel());

        ChannelPre channelPre = new ChannelPre();
        String platOid = csParam.getPlatOid();
        channelPre.setPlatOid(platOid);
        String platParam = csParam.getPlatParam().replaceAll("\"", "");
        String ckid = csParam.getCkid();
        String acid = csParam.getAcid();

        channelPre.setPlatParam(platParam);
        Integer money = csParam.getMoney();
        String remark = "";
        if (money == 200) {
            money = 204;
            remark = "200|204," + ckid + "|" + acid;
        } else if (money == 1) {
            money = 1;
            remark = "1|1," + ckid + "|" + acid + ",test";
        } else if (money == 100) {
            money = 102;
            remark = "100|102," + ckid + "|" + acid;
        }else {
            throw new ServiceException("仅支持100、200的固额设置");
        }
        channelPre.setMoney(money);

        String url = "https://mapi.alipay.com/gateway.do?";

        if (!platParam.contains("_input_charset")) {
            throw new ServiceException("核对queryString参数,示例: _input_charset...");
        }

        String address = url + platParam;
        log.warn("创建预产地址: {}", address);

        channelPre.setAddress(address);

        CAccount caDB = cAccountMapper.getCAccountByAcid(acid);
        String account = caDB.getAcAccount();
        channelPre.setAcAccount(account);
        if (!caDB.getAcAccount().equals(account)) {
            log.warn("give other. {}, {}", caDB.getAcAccount(), account);
            channelPre.setRemark("give other | " + account);
        }
//        CAccount ckAccount = cAccountMapper.getCAccountByAcid(ckid);

//        List<SdoWater> sdoWaters = sdoPayService.queryOrderBy2Day(ckAccount.getAcPwd(), ckid);
//        boolean flag = false;
//        for (SdoWater sdoWater : sdoWaters) {
//            if (sdoWater.getOrderId().equals(platOid)) {
//                boolean moneyFlag = NumberUtil.equals(new BigDecimal(money), new BigDecimal(sdoWater.getOrderAmount()));
//                if (moneyFlag) {
//                    flag = true;
//                }
//            }
//        }
//        if (!flag) throw new ServiceException("核对订单金额");

        channelPre.setUid(uid);
        channelPre.setAcid(acid);
        channelPre.setCkid(ckid);
        channelPre.setChannel(channelDB.getCChannelId());
        channelPre.setCid(channelDB.getId());
        channelPre.setRemark(remark);
        channelPre.setCreateTime(LocalDateTime.now());
        int row = channelPreMapper.insert(channelPre);

        return row;
    }

    @Override
    public ResultOfList<List<ChannelPre>> listChannelPre(ChannelPreParam queryParam) {

        Integer uid = TokenInfoThreadHolder.getToken().getId();
        QueryWrapper<ChannelPre> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(queryParam.getPlatOid())) {
            queryWrapper.eq("plat_oid", queryParam.getPlatOid());
        }
        if (StringUtils.hasLength(queryParam.getChannel())) {
            queryWrapper.likeRight("channel", queryParam.getChannel());
        }
        List<Integer> subList = relationUSMapper.listSidByUid(TokenInfoThreadHolder.getToken().getId());
        subList.add(uid);

        if (queryParam.getStatus() != null) {
            queryWrapper.eq("status", queryParam.getStatus());
        }

        if (queryParam.getMoney() != null) {
            queryWrapper.eq("money", queryParam.getMoney());
        }

        queryWrapper.in("uid", subList);
        queryWrapper.orderByDesc("id");

        Page<ChannelPre> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 20);
        }

        Page<ChannelPre> csPage = channelPreMapper.selectPage(page, queryWrapper);
        List<ChannelPre> records = csPage.getRecords();

        ResultOfList rs = new ResultOfList(records, (int) page.getTotal());
        return rs;
    }


    @Override
    public int updateChannelPre(ChannelPreParam param) {
        ChannelPre channelPre = new ChannelPre();
        channelPre.setId(param.getId());
//        Integer money = param.getMoney();
        String platOid = param.getPlatOid();

        channelPre.setPlatOid(platOid);
        channelPre.setAddress(param.getAddress());

        if (!param.getPlatParam().contains("_input_charset")) {
            throw new ServiceException("核对queryString参数,示例: _input_charset...");
        }

//        String ckid = param.getCkid();
//        CAccount ckAccount = cAccountMapper.getCAccountByAcid(ckid);

//        List<SdoWater> sdoWaters = sdoPayService.queryOrderBy2Day(ckAccount.getAcPwd(), ckid);
//        boolean flag = false;
//        for (SdoWater sdoWater : sdoWaters) {
//            if (sdoWater.getOrderId().equals(platOid)) {
//                boolean moneyFlag = NumberUtil.equals(new BigDecimal(money), new BigDecimal(sdoWater.getOrderAmount()));
//                if (moneyFlag) {
//                    flag = true;
//                }
//            }
//        }
//        if (!flag) throw new ServiceException("核对订单金额");

        int row = channelPreMapper.updateById(channelPre);
        return row;
    }

    @Override
    public int deleteChannelPre(Integer id) {
        int row = channelPreMapper.deleteById(id);
        return row;
    }

    @Override
    public List<CChannel> getChannelPreTypes(ChannelPreParam param) {
        List<CChannel> channels = channelMapper.getChannelPreTypes();
        return channels;
    }
}
