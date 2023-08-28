package com.vbox.service.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.util.RedisUtil;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CChannel;
import com.vbox.persistent.pojo.dto.TxWaterList;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

//@Component
@Slf4j
public class ChannelAccountTxTask {

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

    @Scheduled(cron = "0/5 * * * * ?")   //每 5s 执行一次, 搜集tx 账号
    public void collectTxAccList() {
        log.warn("collectTxAccList. start....");

        List<Integer> cidList = channelMapper.listCID2tx();
        for (Integer cid : cidList) {
            try {
                CChannel cChannel = channelMapper.getChannelById(cid);
                String channel = cChannel.getCChannelId();
                List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                        .eq("status", 1)
                        .eq("sys_status", 1)
                        .eq("cid", cid)
                );

                List<TxWaterList> rl = new ArrayList<>();
                // 使用HashMap来保存相同充值金额的充值账号
                Map<Integer, List<String>> map = new HashMap<>();

                if (randomTempList.size() > 30) {
                    // 随机排序
                    Collections.shuffle(randomTempList);
                    // 截取前30个元素
                    randomTempList = randomTempList.subList(0, 30);
                } else {
                    // 全部取出
                    randomTempList = new ArrayList<>(randomTempList);
                }

                for (CAccount cAccount : randomTempList) {
                    List<TxWaterList> txWaterList = txPayService.queryOrderTXACBy30(cAccount.getAcAccount());
                    rl.addAll(txWaterList);
                }

                log.warn("tx 刷完一次记录, size: {}", rl.size());

                LocalDateTime now = LocalDateTime.now();
                // 遍历rechargeList进行充值金额的筛选
                for (TxWaterList recharge : rl) {
                    Integer payAmt = recharge.getPayAmt() / 100;
                    String provideID = recharge.getProvideID();
                    long payTime = recharge.getPayTime();
                    Instant instant = Instant.ofEpochSecond(payTime);
                    LocalDateTime payTimeLoc = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
                    LocalDateTime pre30min = now.plusMinutes(-30);
                    // 如果该充值金额已存在于结果集中，则将充值账号添加进对应的列表中
                    if (payTimeLoc.isAfter(pre30min)) {
                        List<String> accountList = map.computeIfAbsent(payAmt, k -> new ArrayList<>());
                        accountList.add(provideID);
                    }
                }

                log.warn("处理前- map 集: {}", map);

                List<Integer> moneyList = channelShopMapper.getChannelShopMoneyList(channel);

                for (Integer reqMoney : moneyList) {
                    //确保数据不要超过150个
                    long size = redisUtil.sSize(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
                    log.warn("当前cid的 money - {}, acc池子个数: {}", reqMoney, size);
                    if (size > 200) {
                        redisUtil.del(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
                    }

                    ArrayList<CAccount> currentList = new ArrayList<>(randomTempList);
                    log.warn("处理前.  current list : {}", currentList.size());
                    //获取已经有充值当前金额的账户，作去除处理
                    List<String> qqList = map.get(reqMoney);
                    log.warn("当前30分钟内 - 已支付的记录，金额: {} , 记录：{}", reqMoney, qqList);

                    removeTxElements(qqList, currentList, reqMoney);
                    log.warn("处理后.  current list : {}", currentList.size());

                    for (CAccount cAccount : currentList) {
                        int costExist = pOrderMapper.getPOrderByPre30AndQQ(cAccount.getAcAccount(), reqMoney);
                        if (costExist > 0) {
                            log.warn("当前账号库中已有订单，不入缓存池, acc: {}", cAccount.getAcAccount());
                        }else {
                            redisUtil.sAdd(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney, cAccount);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("当前 channel err： ", e);
            }
        }
        log.warn("collectTxAccList. end....");

    }

    public void removeTxElements(List<String> qqList, List<CAccount> txList, Integer money) {
        Iterator<CAccount> iterator = txList.iterator();

        while (iterator.hasNext()) {
            CAccount cAccount = iterator.next();
            String qq = cAccount.getAcAccount();

            String acid = cAccount.getAcid();
            Integer count = pOrderMapper.isExistPOrderByAcIdAndStatus(acid, money);
            if (count != null && count > 0) {
                log.warn("当前订单里已有 {} 金额未支付, acid: {}, 作去除", money, acid);
                iterator.remove();
            }

            if (qqList == null || qqList.size() == 0) {
                continue;
            }
            // 判断provideID是否包含在array1中的元素中
            if (qqList.contains(qq)) {
                log.warn("当前tx官方半小时内已有 {} 金额存在, acid: {}, 作去除", money, acid);
                iterator.remove();
            }
        }
    }


}
