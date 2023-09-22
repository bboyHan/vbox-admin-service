package com.vbox.service.task;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.CodeUseStatusEnum;
import com.vbox.common.enums.OrderStatusEnum;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.dto.TxWaterList;
import com.vbox.persistent.pojo.vo.PayNotifyVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.SdoPayService;
import com.vbox.service.channel.TxPayService;
import com.vbox.service.channel.impl.Gee4Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

//@Component
@Slf4j
public class OrderTask {

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
    @Autowired
    private VboxProxyMapper vboxProxyMapper;

    @Scheduled(cron = "0/1 * *  * * ? ")   //每 2秒执行一次, 处理成功订单的回调通知
    @Async("scheduleExecutor")
    public void handleCallbackOrder() throws IllegalAccessException {

        Set<Object> set = redisUtil.sGet(CommonConstant.ORDER_CALLBACK_QUEUE);
        if (set == null || set.isEmpty()) {
            return;
        }
        log.info("handleCallbackOrder.start");
        for (Object nextOrder : set) {
            String orderId = nextOrder.toString();
            try {
                Boolean keyAvailable = redisUtil.isKeyAvailable(CommonConstant.CALLBACK_QUEUE + orderId, 2);

                if (keyAvailable == null) {
                    redisUtil.set0Key(CommonConstant.CALLBACK_QUEUE + orderId);

                    PayOrder po = pOrderMapper.getPOrderByOid(orderId);

                    String notify = po.getNotifyUrl();
                    if (notify == null || "".equals(notify)) {
                        log.info("该订单未设置回调地址，放弃通知，orderID：{}", orderId);
                        redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
                        continue;
                    }

                    PayNotifyVO vo = new PayNotifyVO();
                    vo.setOrder_id(orderId);
                    vo.setStatus(1);
                    vo.setCost(po.getCost());
                    String account = po.getPAccount();
                    PAccount pa = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", account));
                    vo.setP_account(account);
                    String sign = CommonUtil.encodeSign(CommonUtil.objToTreeMap(vo), pa.getPKey());
                    vo.setSign(sign);
                    HttpResponse resp = null;
                    String reqBody = JSONObject.toJSONString(vo);
                    log.info("回调请求消息：notify：{}，req body：{}", notify, reqBody);
                    resp = HttpRequest.post(notify)
                            .body(reqBody)
//                            .timeout(5000)
                            .execute();
                    log.info("回调返回信息： http status： {}， resp： {}", resp.getStatus(), resp.body());
//                    redisUtil.set0Key(CommonConstant.CALLBACK_QUEUE + orderId);

                    if (resp.getStatus() == 200) {
                        LocalDateTime callTime = LocalDateTime.now();
                        pOrderMapper.updateCallbackStatusByOIdForSys(orderId, callTime);
                        redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
                        log.info("该订单已回调成功，通知url：{}，orderID：{}", notify, orderId);
                    }
                    log.warn("首次回调, order id -> {}", orderId);
                } else if (keyAvailable) {
                    redisUtil.incrementCount(CommonConstant.CALLBACK_QUEUE + orderId);
                    log.warn("非首次回调当前order id - {}", orderId);
                    PayOrder po = pOrderMapper.getPOrderByOid(orderId);

                    String notify = po.getNotifyUrl();
                    if (notify == null || "".equals(notify)) {
                        log.info("该订单未设置回调地址，放弃通知，orderID：{}", orderId);
                        redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
                        continue;
                    }

                    PayNotifyVO vo = new PayNotifyVO();
                    vo.setOrder_id(orderId);
                    vo.setStatus(1);
                    vo.setCost(po.getCost());
                    String account = po.getPAccount();
                    PAccount pa = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", account));
                    vo.setP_account(account);
                    String sign = CommonUtil.encodeSign(CommonUtil.objToTreeMap(vo), pa.getPKey());
                    vo.setSign(sign);
                    HttpResponse resp = null;

                    String reqBody = JSONObject.toJSONString(vo);
                    log.info("回调请求消息：notify：{}，req body：{}", notify, reqBody);
                    resp = HttpRequest.post(notify)
                            .body(reqBody)
//                            .timeout(5000)
                            .execute();
                    log.info("回调返回信息： http status： {}， resp： {}", resp.getStatus(), resp.body());

                    if (resp.getStatus() == 200) {
                        LocalDateTime callTime = LocalDateTime.now();
                        pOrderMapper.updateCallbackStatusByOIdForSys(orderId, callTime);
                        redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
                        log.info("该订单已回调成功，通知url：{}，orderID：{}", notify, orderId);
                    }
                } else {
                    log.warn("超限回调停止, order id -> {}", orderId);
                    redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
                }
            } catch (Exception e) {
                log.error("回调失败, order id : {}, err: {}", orderId, e);
            }
        }
        log.info("handleCallbackOrder.end");
    }

//    @Scheduled(cron = "0/10 * * * * ? ")   //每 10秒执行一次, 超时处理
    public void handleDelayOrder() {
        Set<?> set = redisUtil.zGet(CommonConstant.ORDER_DELAY_QUEUE);
        if (set == null || set.isEmpty()) {
            return;
        }
        log.info("handleDelayOrder.start");

        for (Object nextOrder : set) {
            DelayTask<PayOrder> delayTask = JSONObject.parseObject(
                    nextOrder.toString(), new TypeReference<DelayTask<PayOrder>>() {
                    }
            );

            PayOrder po = delayTask.getTask();
            String orderId = po.getOrderId();

            try {
                Integer code;

                // 超时内已经支付入库的情况
                CAccountWallet wallet = cAccountWalletMapper.selectOne(new QueryWrapper<CAccountWallet>().eq("oid", orderId));
                if (wallet != null) {
                    code = 2;
                    log.info("handleDelayOrder c account waller is already to DB, info: {}", wallet);
                } else {
                    JSONObject resp = payService.queryOrderForQuery(orderId);
                    JSONObject data = resp.getJSONObject("data");
                    code = data.getInteger("order_status");
                }

                if (code == 0) { // 未支付则设为超时  3
                    int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                    if (row == 1) {
                        boolean pop = redisUtil.zRemove(CommonConstant.ORDER_DELAY_QUEUE, delayTask);
                        if (pop) {
                            log.info("[removed] - current order is expire, oid: {}, task info: {}", orderId, po);
                        }
                    }
                }
                if (code == 2) { // 查询订单已支付  1
                    int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                    if (row == 1) {
                        boolean pop = redisUtil.zRemove(CommonConstant.ORDER_DELAY_QUEUE, delayTask);
                        if (pop) {
                            log.info("[finished] - current order is finished, oid: {}, task info: {}", orderId, po);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("OrderTask.handleDelayOrder", e);
                boolean pop = redisUtil.zRemove(CommonConstant.ORDER_DELAY_QUEUE, delayTask);
                if (pop) {
                    log.info("[removed] - current order is not exist, task info: {}", delayTask);
                }

            }

        }
        log.info("handleDelayOrder.end");
    }

//    @Scheduled(cron = "0 */1 * * * ? ")   //每 1min 执行一次, 未支付单子复核10min前单子
    public void handleUnPayOrder() {

        List<PayOrder> poList = pOrderMapper.listUnPay();
        if (poList == null || poList.size() == 0) return;
        log.info("handleUnPayOrder.start");

        for (PayOrder po : poList) {
            try {
                CAccount caDB = cAccountMapper.getCAccountByAcid(po.getAcId());
                String orderId = po.getOrderId();
                LocalDateTime nowTime = LocalDateTime.now();
                if (po.getCChannelId().contains("jx3")) {
                    if (po.getCChannelId().contains("jx3_alipay_pre")) {
                        String platformOid = po.getPlatformOid();
                        String address = channelPreMapper.getAddressByPlatOid(platformOid);
                        boolean flag = queryJx3Order(address);
                        log.warn("查到jx3 html pay 已付, platformOid : {}", platformOid);
                        if (flag) {
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                            if (row == 1) {
                                // 支付成功后 入库 wallet
                                CAccountWallet w = new CAccountWallet();
                                w.setCaid(caDB.getId());
                                w.setCost(po.getCost());
                                w.setOid(po.getOrderId());
                                w.setCreateTime(nowTime);
                                try {
                                    channelPreMapper.updateByPlatId(platformOid, 1); //update 1
                                    cAccountWalletMapper.insert(w);
                                } catch (Exception ex) {
                                    log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                                }
                                log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                                long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                                if (rowRedis == 1) {
                                    log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                                }
                            }
                        } else {
                            //没查到充值记录
                            LocalDateTime orderTime = po.getCreateTime();
                            LocalDateTime pre5min = nowTime.plusMinutes(-5);
                            if (pre5min.isAfter(orderTime)) { //超5分钟了查到未支付，直接设置为失败单
                                int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                                if (row == 1) {
                                    log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                                }
                            } else {
                                boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                                log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                            }
                        }
                    } else {
                        Thread.sleep(1500L);
                        // 生产
                        JSONObject resp = payService.queryOrderForQuery(orderId);
                        JSONObject data = resp.getJSONObject("data");
                        Integer code = data.getInteger("order_status");
                        //测试
//                Integer code = 2;
                        if (code == 2) { //未支付的订单，查询平台支付成功了
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                            if (row == 1) {
                                // 支付成功后 入库 wallet

                                CAccountWallet w = new CAccountWallet();
                                w.setCaid(caDB.getId());
                                w.setCost(po.getCost());
                                w.setOid(po.getOrderId());
                                w.setCreateTime(nowTime);
                                try {
                                    cAccountWalletMapper.insert(w);
                                } catch (Exception var14) {
                                    log.warn("CAccountWallet 已经入库, err: {}", var14.getMessage());
                                }
                                log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}， 平台数据：{}", po.getOrderId(), data);

                                long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                                if (rowRedis == 1) {
                                    log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                                }
                            }
                        } else {
                            JSONObject orderInfo = data.getJSONObject("order_info");
                            String rechargeTime = orderInfo.getString("recharge_time");
                            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime orderTime = LocalDateTime.parse(rechargeTime, format);
                            LocalDateTime pre5min = nowTime.plusMinutes(-5);
                            if (pre5min.isAfter(orderTime)) { //超5分钟了查到未支付，直接设置为失败单
                                int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                                if (row == 1) {
                                    log.info("[task check] not pay order, check platform pay timeout, pay order: {}, platform order info: {}", po, data);
                                }
                            }
                        }
                    }

                } else if (po.getCChannelId().contains("tx")) {//
                    Integer money = po.getCost();
//                    String openID = caDB.getAcPwd();
//                    String openKey = caDB.getCk();
                    String qq = caDB.getAcAccount();

//                    List<TxWaterList> txWaterLists = txPayService.queryOrderBy30(openID, openKey);
                    List<TxWaterList> txWaterLists = txPayService.queryOrderTXACBy30(qq);

                    // 使用HashMap来保存相同充值金额的充值账号
                    Map<Integer, List<String>> map = new HashMap<>();

                    // 遍历rechargeList进行充值金额的筛选
                    for (TxWaterList recharge : txWaterLists) {
                        Integer payAmt = recharge.getPayAmt() / 100;
                        String provideID = recharge.getProvideID();
                        long payTime = recharge.getPayTime();
                        Instant instant = Instant.ofEpochSecond(payTime);
                        LocalDateTime payTimeLoc = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
                        LocalDateTime pre20min = payTimeLoc.plusMinutes(20);
                        LocalDateTime createTime = po.getCreateTime();
                        LocalDateTime pre5min = createTime.plusMinutes(-5);
                        // 如果该充值金额已存在于结果集中，则将充值账号添加进对应的列表中
                        if (payTimeLoc.isBefore(pre20min) && payTimeLoc.isAfter(pre5min)) {
                            List<String> accountList = map.computeIfAbsent(payAmt, k -> new ArrayList<>());
                            accountList.add(provideID);
                            log.warn("payTimeLoc.isAfter(pre30min) - qq: {},记录：{}", provideID, recharge);
                        }
                    }

                    List<String> moneyQQList = map.get(money);
                    if (moneyQQList == null || moneyQQList.size() == 0) {
                        //没查到充值记录
                        LocalDateTime orderTime = po.getCreateTime();
                        LocalDateTime pre20min = nowTime.plusMinutes(-20);
//                        log.warn("订单时间： {}, 比较20分钟前的时间：{}", orderTime, pre20min);
                        if (pre20min.isAfter(orderTime)) { //超20分钟了查到未支付，直接设置为失败单
                            log.warn("订单创建时间：{}, 当前时间： {}， 超时未查询到支付记录 order id : {}", orderTime, nowTime, orderId);
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                            if (row == 1) {
                                log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                            }
                        }
                    } else {
                        if (moneyQQList.contains(qq)) { // 支付成功的
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                            if (row == 1) {
                                // 支付成功后 入库 wallet
                                CAccountWallet w = new CAccountWallet();
                                w.setCaid(caDB.getId());
                                w.setCost(po.getCost());
                                w.setOid(po.getOrderId());
                                w.setCreateTime(nowTime);
                                try {
                                    cAccountWalletMapper.insert(w);
                                } catch (Exception ex) {
                                    log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                                }
                                log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                                long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                                if (rowRedis == 1) {
                                    log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                                }
                            }
                        } else {
                            //没查到充值记录
                            LocalDateTime orderTime = po.getCreateTime();
                            LocalDateTime pre10min = nowTime.plusMinutes(-20);
//                            log.warn("moneyQQList - 订单时间： {}, 比较10分钟前的时间：{}", orderTime, pre10min);
                            if (pre10min.isAfter(orderTime)) { //超10分钟了查到未支付，直接设置为失败单
                                log.warn("订单创建时间：{}, 当前时间： {}， 超时未查询到支付记录 order id : {}", orderTime, nowTime, orderId);
                                int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                                if (row == 1) {
                                    log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                                }
                            }
                        }
                    }

                } else if (po.getCChannelId().contains("sdo")) {

                    if (!po.getCChannelId().contains("sdo_in")) {
                        String platformOid = po.getPlatformOid();
                        String address = channelPreMapper.getAddressByPlatOid(platformOid);
                        boolean flag = querySdoOrder(address);
                        log.warn("查到sdo html pay 已付, platformOid : {}", platformOid);
                        if (flag) {
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                            if (row == 1) {
                                // 支付成功后 入库 wallet
                                CAccountWallet w = new CAccountWallet();
                                w.setCaid(caDB.getId());
                                w.setCost(po.getCost());
                                w.setOid(po.getOrderId());
                                w.setCreateTime(nowTime);
                                try {
                                    channelPreMapper.updateByPlatId(platformOid, 1); //update 1
                                    cAccountWalletMapper.insert(w);
                                } catch (Exception ex) {
                                    log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                                }
                                log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                                long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                                if (rowRedis == 1) {
                                    log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                                }
                            }
                        }
//
                        else {
                            //没查到充值记录
                            LocalDateTime orderTime = po.getCreateTime();
                            LocalDateTime pre4min = nowTime.plusMinutes(-4);
                            if (pre4min.isAfter(orderTime)) { //超4分钟了查到未支付，直接设置为失败单
                                int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                                if (row == 1) {
                                    log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                                }
                            } else {
                                boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                                log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                            }
                        }
                    }

                }

            } catch (Exception e) {
                log.error("OrderTask. po: {}, handleUnPayOrder", po, e);
            }
        }
        log.info("handleUnPayOrder.end");


    }

    public boolean queryJx3Order(String address) {
        try {
            HttpResponse execute = HttpRequest.get(address)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .execute();
            String location = execute.header("Location");

            HttpResponse executeLocation = HttpRequest.get(location)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .execute();

            String html = executeLocation.body();
            return html.contains("交易已经支付") || html.contains("该订单已付款");
        } catch (Exception e) {
            log.error("queryJx3Order ex: ", e);
            return false;
        }
    }

    public boolean querySdoOrder(String address) {
        try {
            HttpResponse execute = HttpRequest.get(address)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .execute();
            String location = execute.header("Location");

            HttpResponse executeLocation = HttpRequest.get(location)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .execute();

            String html = executeLocation.body();
            return html.contains("交易已经支付") || html.contains("该订单已付款");
        } catch (Exception e) {
            log.error("querySdoOrder ex: ", e);
            return false;
        }
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

    public boolean queryWMEOrder(String address) {
        try {
            HttpResponse execute = HttpRequest.get(address)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .execute();
            String location = execute.header("Location");

            HttpResponse executeLocation = HttpRequest.get(location)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                    .execute();

            String html = executeLocation.body();
            return html.contains("交易已经支付") || html.contains("该订单已付款");
        } catch (Exception e) {
            log.error("queryWMEOrder ex: ", e);
            return false;
        }
    }

    @Scheduled(cron = "0/3 * * * * ?")   //每 2s 执行一次, 未支付单子复核 redis 池子
    public void handleAsyncUnPayOrder() {
        Object ele = redisUtil.sPop(CommonConstant.ORDER_QUERY_QUEUE);
        if (ele == null) {
            return;
        }
        log.info("OrderCreateTask.handleUnPayOrder.start");
        String text = ele.toString();
        PayOrder po = null;
        try {
            po = JSONObject.parseObject(text, PayOrder.class);
        } catch (Exception e) {
            log.error("handleAsyncUnPayOrder.order解析异常： {}", text);
            return;
        }
        LocalDateTime nowTime = LocalDateTime.now();
        try {
            String orderId = po.getOrderId();
            CAccount caDB = cAccountMapper.getCAccountByAcid(po.getAcId());
            String cChannelId = po.getCChannelId();
            if (cChannelId.contains("jx3")) {
                if (cChannelId.contains("jx3_alipay_pre")) {
                    //                payService.addProxy(null, po.getPayIp(), null);
                    String platformOid = po.getPlatformOid();
                    log.warn("预产自动查单任务开始执行- channel: {}, plat oid : {}", cChannelId, platformOid);
                    String address = channelPreMapper.getAddressByPlatOid(platformOid);
                    boolean flag = queryJx3Order(address);
                    if (flag) {
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                        if (row == 1) {
                            // 支付成功后 入库 wallet
                            CAccountWallet w = new CAccountWallet();
                            w.setCaid(caDB.getId());
                            w.setCost(po.getCost());
                            w.setOid(po.getOrderId());
                            w.setCreateTime(nowTime);
                            try {
                                channelPreMapper.updateByPlatId(platformOid, 1); //update 1
                                cAccountWalletMapper.insert(w);
                            } catch (Exception ex) {
                                log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                            }
                            log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                            long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                            if (rowRedis == 1) {
                                log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                            }
                        }
                    } else {
                        //没查到充值记录
                        LocalDateTime orderTime = po.getCreateTime();
                        LocalDateTime pre4min = nowTime.plusMinutes(-4);
                        if (pre4min.isAfter(orderTime)) { //超10分钟了查到未支付，直接设置为失败单
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                            if (row == 1) {
                                log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                            }
                        } else {
                            boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                            log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                        }
                    }
                    return;
                } else {
                    // 生产
                    log.warn("query pay order ip: {}", po.getPayIp());
                    payService.addProxy(null, po.getPayIp(), null);
                    JSONObject resp = payService.queryOrder(orderId);
//            JSONObject resp = payService.queryOrderForQuery(orderId);
                    JSONObject data = resp.getJSONObject("data");
                    Integer code = data.getInteger("order_status");
                    //测试
//                Integer code = 2;
                    if (code == 2) { //未支付的订单，查询平台支付成功了
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                        if (row == 1) {
                            // 支付成功后 入库 wallet
                            CAccount ca = cAccountMapper.getCAccountByAcid(po.getAcId());

                            CAccountWallet w = new CAccountWallet();
                            w.setCaid(ca.getId());
                            w.setCost(po.getCost());
                            w.setOid(po.getOrderId());
                            w.setCreateTime(nowTime);
                            try {
                                cAccountWalletMapper.insert(w);
                            } catch (Exception var14) {
                                log.warn("CAccountWallet 已经入库, err: {}", var14.getMessage());
                            }
                            log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}， 平台数据：{}", po.getOrderId(), data);

                            long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                            if (rowRedis == 1) {
                                log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                            }

                        }
                    } else {
                        JSONObject orderInfo = data.getJSONObject("order_info");
                        String rechargeTime = orderInfo.getString("recharge_time");
                        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        LocalDateTime orderTime = LocalDateTime.parse(rechargeTime, format);
                        LocalDateTime pre3min = nowTime.plusMinutes(-3);
                        if (pre3min.isAfter(orderTime)) { //超3分钟了查到未支付，直接设置为失败单
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                            if (row == 1) {
                                log.info("【任务执行】 not pay order, 订单超时置为超时状态, pay order: {}, platform order info: {}", po, data);
                            }
                        } else {
                            boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                            log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                        }
                    }
                }
            } else if (cChannelId.contains("xoy")) { // c_channel_id = xoy
                log.warn("xoyo 自动查单任务开始执行- channel: {}", cChannelId);

//                payService.addProxy(null, po.getPayIp(), null);
                payService.addProxy(null, "127.0.0.1", null);

                Integer money = po.getCost();

                CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(caDB.getCid(), caDB.getGid());
//                Integer currentBalance = payService.getBalance(cgi.getCGateway(), cookie, realPwd);
                Integer currentBalance = payService.getBalance2JXAcc(cgi.getCGateway(), caDB);

                Integer preBalance = (Integer) redisUtil.get(CommonConstant.CHANNEL_ACCOUNT_BALANCE + caDB.getAcid() + "|" + caDB.getAcAccount());
                log.warn("preBalance - {}, current balance : {}, acc info : {}", preBalance, currentBalance, caDB.getAcAccount());
                if (currentBalance - preBalance == money * 100) { // 判断为充值成功
                    int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                    if (row == 1) {
                        // 支付成功后 入库 wallet
                        CAccountWallet w = new CAccountWallet();
                        w.setCaid(caDB.getId());
                        w.setCost(po.getCost());
                        w.setOid(po.getOrderId());
                        w.setCreateTime(nowTime);
                        try {
                            cAccountWalletMapper.insert(w);
                        } catch (Exception ex) {
                            log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                        }
                        log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                        long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                        if (rowRedis == 1) {
                            log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                        }
                    }
                }else {
                    //没查到充值记录
                    LocalDateTime orderTime = po.getCreateTime();
                    LocalDateTime pre8min = nowTime.plusMinutes(-8);
                    if (pre8min.isAfter(orderTime)) { //超8分钟了查到未支付，直接设置为失败单
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                        if (row == 1) {
                            log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                        }
                    } else {
                        boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                        log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                    }
                }
            } else if (cChannelId.contains("tx")) { // c_channel_id = tx

//                Thread.sleep(1000);
                log.warn("tx 自动查单任务开始执行- channel: {}", cChannelId);

                payService.addProxy(null, "127.0.0.1", null);

                Integer money = po.getCost();
                String qq = caDB.getAcAccount();

                List<TxWaterList> txWaterLists = txPayService.queryOrderBy30(caDB.getAcPwd(), caDB.getCk());

                // 使用HashMap来保存相同充值金额的充值账号
                Map<Integer, List<String>> map = new HashMap<>();

                //订单创建时间
                LocalDateTime orderCreateTime = po.getCreateTime();

                // 遍历rechargeList进行充值金额的筛选
                for (TxWaterList recharge : txWaterLists) {

                    Integer payAmt = recharge.getPayAmt() / 100;
                    String provideID = recharge.getProvideID();
                    long payTime = recharge.getPayTime();
                    Instant instant = Instant.ofEpochSecond(payTime);
                    //订单支付时间
                    LocalDateTime payTimeLoc = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
                    //订单创建后的30分钟内
                    LocalDateTime after20min = orderCreateTime.plusMinutes(20);
                    LocalDateTime pre5min = orderCreateTime.plusMinutes(-5);

                    // 如果该充值金额已存在于结果集中，则将充值账号添加进对应的列表中
                    if (payTimeLoc.isBefore(after20min) && payTimeLoc.isAfter(pre5min)) { //订单在创建时间前5分钟到后20分钟内支付过
                        List<String> accountList = map.computeIfAbsent(payAmt, k -> new ArrayList<>());
                        accountList.add(provideID);
                        log.warn("payTimeLoc.isAfter(pre30min) - qq: {},记录：{}", provideID, recharge);
                    }
                }

                List<String> moneyQQList = map.get(money);
                log.warn("OrderTask: 当前qq : {}", qq);
                log.warn("OrderTask: 当前money: {}, collect : {}", money, moneyQQList);
                if (moneyQQList == null || moneyQQList.size() == 0) {
                    //没查到充值记录
                    LocalDateTime after12min = nowTime.plusMinutes(-20);
                    if (after12min.isAfter(orderCreateTime)) { //超20分钟了查到未支付，直接设置为失败单
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                        if (row == 1) {
                            log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                        }
                    } else {
                        boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                        log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                    }
                } else {
                    if (moneyQQList.contains(qq)) { // 支付成功的
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                        if (row == 1) {
                            // 支付成功后 入库 wallet
                            CAccountWallet w = new CAccountWallet();
                            w.setCaid(caDB.getId());
                            w.setCost(po.getCost());
                            w.setOid(po.getOrderId());
                            w.setCreateTime(nowTime);
                            try {
                                cAccountWalletMapper.insert(w);
                            } catch (Exception ex) {
                                log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                            }
                            log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                            long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                            if (rowRedis == 1) {
                                log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                            }
                        }
                    } else {
                        //没查到充值记录
                        LocalDateTime orderTime = po.getCreateTime();
                        LocalDateTime pre12min = nowTime.plusMinutes(-12);
                        if (pre12min.isAfter(orderTime)) { //超12分钟了查到未支付，直接设置为失败单
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                            if (row == 1) {
                                log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                            }
                        } else {
                            boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                            log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                        }
                    }
                }
            } else if (cChannelId.contains("sdo")) {
                if (!cChannelId.contains("sdo_in")) {
                    String platformOid = po.getPlatformOid();
                    log.warn("预产自动查单任务开始执行- channel: {}, plat oid : {}", cChannelId, platformOid);

                    String address = channelPreMapper.getAddressByPlatOid(platformOid);
                    boolean flag = querySdoOrder(address);
                    if (flag) {
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                        if (row == 1) {
                            // 支付成功后 入库 wallet
                            CAccountWallet w = new CAccountWallet();
                            w.setCaid(caDB.getId());
                            w.setCost(po.getCost());
                            w.setOid(po.getOrderId());
                            w.setCreateTime(nowTime);
                            try {
                                channelPreMapper.updateByPlatId(platformOid, 1); //update 1
                                cAccountWalletMapper.insert(w);
                            } catch (Exception ex) {
                                log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                            }
                            log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                            long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                            if (rowRedis == 1) {
                                log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                            }
                        }
                    } else {
                        //没查到充值记录
                        LocalDateTime orderTime = po.getCreateTime();
                        LocalDateTime pre4min = nowTime.plusMinutes(-4);
                        if (pre4min.isAfter(orderTime)) { //超10分钟了查到未支付，直接设置为失败单
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                            if (row == 1) {
                                log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                            }
                        } else {
                            boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                            log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                        }
                    }
                }else {
                    payService.addProxy(null, "127.0.0.1", null);

                    String platformOid = po.getPlatformOid();
                    log.warn("预产自动查单任务开始执行- channel: {}, plat oid : {}", cChannelId, platformOid);

                    boolean flag = querySdoInOrder(orderId);
                    if (flag) {
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                        if (row == 1) {
                            // 支付成功后 入库 wallet
                            CAccountWallet w = new CAccountWallet();
                            w.setCaid(caDB.getId());
                            w.setCost(po.getCost());
                            w.setOid(po.getOrderId());
                            w.setCreateTime(nowTime);
                            try {
                                channelPreMapper.updateByPlatId(platformOid, 1); //update 1
                                cAccountWalletMapper.insert(w);
                            } catch (Exception ex) {
                                log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                            }
                            log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                            long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                            if (rowRedis == 1) {
                                log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                            }
                        }
                    } else {
                        //没查到充值记录
                        LocalDateTime orderTime = po.getCreateTime();
                        LocalDateTime pre4min = nowTime.plusMinutes(-4);
                        if (pre4min.isAfter(orderTime)) { //超4分钟了查到未支付，直接设置为失败单
                            int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                            if (row == 1) {
                                log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                            }
                        } else {
                            boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                            log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                        }
                    }
                }
            } else if (cChannelId.contains("wme")) {
                //                payService.addProxy(null, po.getPayIp(), null);

                String platformOid = po.getPlatformOid();
                log.warn("wme 自动查单任务开始执行- channel: {}, plat oid : {}", cChannelId, platformOid);

                PayOrderEvent event = pOrderEventMapper.getPOrderEventByOid(orderId);
                String address = event.getExt();
                boolean flag = queryWMEOrder(address);
                if (flag) {
                    int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                    if (row == 1) {
                        // 支付成功后 入库 wallet
                        CAccountWallet w = new CAccountWallet();
                        w.setCaid(caDB.getId());
                        w.setCost(po.getCost());
                        w.setOid(po.getOrderId());
                        w.setCreateTime(nowTime);
                        try {
                            channelPreMapper.updateByPlatId(platformOid, 1); //update 1
                            cAccountWalletMapper.insert(w);
                        } catch (Exception ex) {
                            log.warn("CAccountWallet 已经入库, err: {}", ex.getMessage());
                        }
                        log.info("[task check] 自动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}", po.getOrderId());
                        long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);
                        if (rowRedis == 1) {
                            log.info("handleUnPayOrder, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                        }
                    }
                } else {
                    //没查到充值记录
                    LocalDateTime orderTime = po.getCreateTime();
                    LocalDateTime pre4min = nowTime.plusMinutes(-4);
                    if (pre4min.isAfter(orderTime)) { //超10分钟了查到未支付，直接设置为失败单
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                        if (row == 1) {
                            log.info("[task check] not pay order, check platform pay timeout, pay order: {}", po);
                        }
                    } else {
                        boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                        log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                    }
                }
            }


        } catch (NotFoundException ex) {
            log.error("订单不存在异常：", ex);
            pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
            log.error("【任务执行】handleUnPayOrder数据库置为异常单, orderId : {}", po.getOrderId());
        } catch (Exception e) {
            LocalDateTime createTime = po.getCreateTime();
            LocalDateTime pre30min = nowTime.plusMinutes(-30);
            if (pre30min.isAfter(createTime)) {
                log.error("【任务执行】handleUnPayOrder超时30分钟直接丢弃, {}", po);
                pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
                log.error("【任务执行】handleUnPayOrder数据库置为异常单, orderId : {}", po.getOrderId());
            } else {
                if (po != null) {
                    boolean b = redisUtil.sAdd(CommonConstant.ORDER_QUERY_QUEUE, po);
                    log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                }
            }
            log.error("OrderCreateTask.handleUnPayOrder", e);
        }
        log.info("OrderCreateTask.handleAsyncUnPayOrder.end");
    }

    private boolean querySdoInOrder(String orderId) {

        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        CAccount ca = cAccountMapper.getCAccountByAcid(po.getAcId());
        String account = ca.getAcAccount();
        String acPwd = Base64.decodeStr(ca.getAcPwd());

        String sdoLoginUrl = vboxProxyMapper.getEnvUrl("sdo_in_query");

        String cookie = ca.getCk();

        JSONObject dJson = new JSONObject();

        String platformOid = po.getPlatformOid().split("\\|")[0];
        log.warn("order id : {}, 处理后的 plat id: {}", orderId, platformOid);
        dJson.put("queryCode", platformOid);
        dJson.put("tgt", cookie);
        dJson.put("proxy", ProxyInfoThreadHolder.getAddress());
        String param = dJson.toJSONString();

        log.warn("sdo in 查单参数： {}", param);
        String qryResp = HttpRequest.post(sdoLoginUrl)
                .body(param)
                .execute().body();

        JSONObject qryRes = JSONObject.parseObject(qryResp);
        if (qryRes.getInteger("state") == 1) {
            Integer paidCount = qryRes.getInteger("paidCount");
            return paidCount == 1;
        } else {

            // 从库里那的ck 校验一次
            boolean expire = sdoPayService.tokenCheckInner(cookie);

            if (!expire) {
                cookie = sdoPayService.getInnerCK(account, acPwd); //有可能从缓存拿一次ck
                expire = sdoPayService.tokenCheckInner(cookie);
                if (expire) {
                    cAccountMapper.updateCkByID(cookie, ca.getId());
                } else { //假设缓存ck过期，删掉，重新官方拿
                    redisUtil.del(CommonConstant.ACCOUNT_CK + ca.getAcAccount());
                    cookie = sdoPayService.getInnerCK(account, acPwd);
                    expire = sdoPayService.tokenCheckInner(cookie);
                    if (expire) {
                        cAccountMapper.updateCkByID(cookie, ca.getId());
                    } else {
                        log.error("ck 更新失败");
//                                cAccountMapper.stopByCaId("ck或密码有误，请更新", c.getId());
                        throw new ServiceException("ck问题，请联系管理员");
                    }
                }
            }

            log.error("查单异常 sdo in， info res: {}", qryResp);
            throw new ServiceException("查单异常 sdo in， info res");
        }
    }

}
