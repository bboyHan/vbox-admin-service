package com.vbox.service.task;

import cn.hutool.core.codec.Base64;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.SdoPayService;
import com.vbox.service.channel.TxPayService;
import com.vbox.service.channel.impl.Gee4Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//@Component
@Slf4j
public class ChannelAccountXOYTask {

    @Resource
    private RedisUtil redisUtil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VboxUserWalletMapper vboxUserWalletMapper;
    @Autowired
    private CAccountMapper cAccountMapper;
    @Autowired
    private CAccountWalletMapper cAccountWalletMapper;
    @Autowired
    private POrderEventMapper pOrderEventMapper;
    @Autowired
    private POrderMapper pOrderMapper;
    @Autowired
    private PayService payService;
    @Autowired
    private Gee4Service gee4Service;
    @Autowired
    private CGatewayMapper cGatewayMapper;
    @Autowired
    private PAccountMapper pAccountMapper;
    @Autowired
    private SdoPayService sdoPayService;
    @Autowired
    private ChannelShopMapper channelShopMapper;
    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private ChannelPreMapper channelPreMapper;
    @Autowired
    private TxPayService txPayService;

    @Scheduled(cron = "0/10 * * * * ?")   //每 10s 执行一次, 搜集xoy 账号
    public void collectTxAccList() {
        log.warn("collect XOY AccList. start....");

        List<Integer> cidList = channelMapper.listCID2XOY();
        for (Integer cid : cidList) {
            try {
//                CChannel cChannel = channelMapper.getChannelById(cid);
                List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                        .eq("status", 1)
                        .eq("sys_status", 1)
                        .eq("cid", cid)
                );

                if (randomTempList.size() > 30) {  //京ICP备 2022012518号-1
                    // 随机排序
                    Collections.shuffle(randomTempList);
                    // 截取前30个元素
                    randomTempList = randomTempList.subList(0, 30);
                } else {
                    // 全部取出
                    randomTempList = new ArrayList<>(randomTempList);
                }

                for (CAccount cAccount : randomTempList) {
                    try {//确保数据不要超过30个
                        long size = redisUtil.sSize(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid);
                        log.warn("当前cid - {}, acc池子个数: {}", cid, size);
                        if (size > 30) {
                            redisUtil.del(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid);
                            log.warn("超池子，控制清除...");
                        }

                        int costExist = pOrderMapper.getPOrderByPre8AndXoyAcc(cAccount.getAcAccount());
                        if (costExist > 0) {
                            log.warn("当前账号库中已有订单，不入缓存池, acc: {}", cAccount.getAcAccount());
                        } else {
//                            payService.addProxy(null, "127.0.0.1", null);
//                            CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(cAccount.getCid(), cAccount.getGid());
//
//                            String cookie = "";
//                            String account = cAccount.getAcAccount();
//                            String acPwd = cAccount.getAcPwd();
//                            String readPwd = Base64.decodeStr(acPwd);
//                            cookie = payService.getCK(account, readPwd);
//                            boolean expire = gee4Service.tokenCheck(cookie, account);
//                            if (!expire) {
//                                redisUtil.del("account:ck:" + account);
//                                cookie = payService.getCK(account, readPwd);
//                                expire = gee4Service.tokenCheck(cookie, account);
//                                if (!expire) {
////                                    cAccountMapper.stopByCaId("ck或密码有误，请更新", cAccount.getId());
//                                    throw new NotFoundException("ck问题，请联系管理员");
//                                }
//                            }
//                            Integer currentBalance = payService.getBalance(cgi.getCGateway(), cookie, readPwd);
//                            if (currentBalance != null) {
//                                redisUtil.sAdd(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid, cAccount);
//                                redisUtil.set(CommonConstant.CHANNEL_ACCOUNT_BALANCE + cAccount.getAcid() + "|" + cAccount.getAcAccount(), currentBalance);
//                                log.warn("当前账号入缓存池, acc: {}, 当前最新余额: {}", cAccount.getAcAccount(), currentBalance);
//                            }
                            redisUtil.sAdd(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid, cAccount);
                        }
                    } catch (Exception e) {
//                        cAccountMapper.stopByCaId(e.toString(), cAccount.getId());
                        log.error("当前 acc 入池 err： ", e);
                    }
                }
            } catch (Exception e) {
                log.error("当前 channel err： ", e);
            }
        }
        log.warn("collect XOY AccList. end....");

    }

}
