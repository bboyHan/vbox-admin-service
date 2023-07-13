package com.vbox.service.task;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.Result;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.CodeUseStatusEnum;
import com.vbox.common.enums.OrderCallbackEnum;
import com.vbox.common.enums.OrderStatusEnum;
import com.vbox.common.enums.ResultEnum;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.CAccountInfo;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.dto.POrderQueue;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.pojo.vo.PayNotifyVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    //    @Scheduled(cron = "0 */1 * * * ?")
    public void handleUserBalance() {
        List<User> users = userMapper.selectList(null);

        for (User user : users) {
            Integer uid = user.getId();
            // 总账户充值
            Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

            // 总订单充值（花费）
            Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);

            totalRecharge = totalRecharge == null ? 0 : totalRecharge;
            totalCost = totalCost == null ? 0 : totalCost;
            // 总余额
            int balance = totalRecharge - totalCost;

            if (balance <= 0) {
                cAccountMapper.stopByUid("系统监测余额不足，请联系管理员确认", uid);
            }

            if (balance > 500 && balance < 5000) {
                cAccountMapper.startByUid("账户余额不足5000，请注意使用", uid);
            }

            if (balance > 5000) {
                cAccountMapper.startByUid("账户可正常使用，请随时查看状态", uid);
            }
        }

        QueryWrapper<CAccount> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("status", 1);
        queryWrapper.eq("sys_status", 1);
        List<CAccount> acList = cAccountMapper.selectList(queryWrapper);

        for (CAccount ca : acList) {
            Integer dailyLimit = ca.getDailyLimit();
            Integer totalLimit = ca.getTotalLimit();

            Integer acID = ca.getId();
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(acID);
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(acID);

            if (dailyLimit != null && dailyLimit > 0 && todayCost != null && todayCost >= dailyLimit) {
                cAccountMapper.stopByCaId("已超出日内限额，账号关闭", acID);
            }

            if (totalLimit != null && totalLimit > 0 && totalCost != null && totalCost >= totalLimit) {
                cAccountMapper.stopByCaId("已超出总限额控制，账号关闭", acID);
            }
        }
    }

    @Scheduled(cron = "0/2 * *  * * ? ")   //每 2秒执行一次, 处理成功订单的回调通知
    @Async("scheduleExecutor")
    public void handleCallbackOrder() throws IllegalAccessException {

        Set<Object> set = redisUtil.sGet(CommonConstant.ORDER_CALLBACK_QUEUE);
        if (set == null || set.isEmpty()) {
            return;
        }
        log.info("handleCallbackOrder.start");
        for (Object nextOrder : set) {
            String orderId = nextOrder.toString();
            PayOrder po = pOrderMapper.getPOrderByOid(orderId);

//            try {
//                //生产
//                JSONObject resp = payService.queryOrder(orderId);
//                JSONObject data = resp.getJSONObject("data");
//                Integer code = data.getInteger("order_status");
//                //测试
//                //Integer code = 1;
//                if (code == 0) { // 告知 支付成功，但查询平台，发现未支付，则改为支付失败 0
//                    pOrderMapper.updateStatusByOIdWhenCall(orderId, OrderStatusEnum.PAY_FAILED.getCode(), CodeStatusEnum.PLATFORM_NOT_PAY.getCode());
//                }
//
//                if (code == 2) { // 告知 支付成功，查询平台已支付，则改为支付成功 1
//                    pOrderMapper.updateStatusByOIdWhenCall(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeStatusEnum.FINISHED.getCode());
//
//                    OrderCallbackVO vo = new OrderCallbackVO();
//                    vo.setPay_status(OrderStatusEnum.PAY_FINISHED.getCode());
//                    vo.setPay_desc(OrderStatusEnum.PAY_FINISHED.getMsg());
//                    vo.setMsg(CodeStatusEnum.FINISHED.getMsg());
//                    vo.setP_order_id(orderId);
//
//                    //支付入库 wallet
//                    String acId = po.getAcId();
//                    CAccount ca = cAccountMapper.selectOne(new QueryWrapper<CAccount>().eq("acid", acId));
//
//                    CAccountWallet w = new CAccountWallet();
//                    w.setOid(orderId);
//                    w.setCaid(ca.getId());
//                    w.setCreateTime(LocalDateTime.now());
//                    w.setCost(po.getCost());
//                    cAccountWalletMapper.insert(w);
//
//
//                }
//            } catch (Exception e) {
//                log.error("handleCallbackOrder", e);
//            }finally {
//                long row = redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
//                if (row == 0) {
//                    log.info("[call back success], order info: {}", po);
//                }
//            }

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
            try {
                String reqBody = JSONObject.toJSONString(vo);
                log.info("回调请求消息：notify：{}，req body：{}", notify, reqBody);
                this.redisUtil.pub(String.format("回调请求消息：notify：%s，req body：%s", notify, reqBody));
                resp = HttpRequest.post(notify)
                        .body(reqBody)
                        .execute();
                log.info("回调返回信息： http status： {}， resp： {}", resp.getStatus(), resp.body());
                this.redisUtil.pub(String.format("回调返回信息： http status： %s， resp： %s", resp.getStatus(), resp.body()));
                if (resp.getStatus() == 200) {
                    LocalDateTime callTime = LocalDateTime.now();
                    pOrderMapper.updateCallbackStatusByOIdForSys(orderId, callTime);
                    redisUtil.setRemove(CommonConstant.ORDER_CALLBACK_QUEUE, orderId);
                    log.info("该订单已回调成功，通知url：{}，orderID：{}", notify, orderId);
                }
            } catch (Exception e) {
                log.error("回调失败，notify: {}, resp: {}, err: {}", notify, resp, e);
            }
        }
        log.info("handleCallbackOrder.end");

    }

    @Scheduled(cron = "0/10 * * * * ? ")   //每 10秒执行一次, 超时处理
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

    @Scheduled(cron = "0 */1 * * * ? ")   //每 1min 执行一次, 未支付单子复核10min前单子
    public void handleUnPayOrder() {

        List<PayOrder> poList = pOrderMapper.listUnPay();
        if (poList == null || poList.size() == 0) return;
        log.info("handleUnPayOrder.start");

        for (PayOrder po : poList) {
            try {
                String orderId = po.getOrderId();
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
                        CAccount ca = cAccountMapper.getCAccountByAcid(po.getAcId());

                        CAccountWallet w = new CAccountWallet();
                        w.setCaid(ca.getId());
                        w.setCost(po.getCost());
                        w.setOid(po.getOrderId());
                        w.setCreateTime(LocalDateTime.now());
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
                    LocalDateTime nowTime = LocalDateTime.now().plusMinutes(-5);
                    if (nowTime.isAfter(orderTime)) { //超5分钟了查到未支付，直接设置为失败单
                        int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                        if (row == 1) {
                            log.info("[task check] not pay order, check platform pay timeout, pay order: {}, platform order info: {}", po, data);
                        }

                    }
                }
            } catch (Exception e) {
                log.error("OrderTask. po: {}, handleUnPayOrder", po, e);
            }
        }
        log.info("handleUnPayOrder.end");


    }

    @Scheduled(cron = "0/5 * * * * ?")   //每 2s 执行一次, 未支付单子复核 redis 池子
    public void handleAsyncUnPayOrder() {
        Object ele = redisUtil.rPop(CommonConstant.ORDER_QUERY_QUEUE);
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
        try {
            String orderId = po.getOrderId();
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
                    w.setCreateTime(LocalDateTime.now());
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
                LocalDateTime nowTime = LocalDateTime.now().plusMinutes(-3);
                if (nowTime.isAfter(orderTime)) { //超5分钟了查到未支付，直接设置为失败单
                    int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_TIMEOUT.getCode(), CodeUseStatusEnum.PLATFORM_NOT_PAY.getCode());
                    if (row == 1) {
                        log.info("【任务执行】 not pay order, 订单超时置为超时状态, pay order: {}, platform order info: {}", po, data);
                    }
                } else {
                    boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, po);
                    log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
                }
            }
        } catch (NotFoundException ex){
            log.error("订单不存在异常：", ex);
        } catch (Exception e) {
//            LocalDateTime createTime = po.getCreateTime();
//            LocalDateTime nowTime = LocalDateTime.now().plusMinutes(-3);
//            if (nowTime.isAfter(createTime)) {
//                log.error("【任务执行】handleUnPayOrder超时3分钟直接丢弃, {}", po);
//                pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
//                log.error("【任务执行】handleUnPayOrder数据库置为异常单, orderId : {}", po.getOrderId());
//            } else {
            if (po != null) {
                boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, po);
                log.error("【任务执行】handleUnPayOrder重新丢回队列, {}, push: {}", po.getOrderId(), b);
            }
//            }
            log.error("OrderCreateTask.handleUnPayOrder", e);
        }
        log.info("OrderCreateTask.handleAsyncUnPayOrder.end");
    }

    //    @Scheduled(cron = "0/5 * * * * ?")   //每 5s 执行一次, 接受订单创建的队列
//    @Async("scheduleExecutor")
    public void handleAsyncCreateOrder() {
        Object ele = redisUtil.rPop(CommonConstant.ORDER_CREATE_QUEUE);
        if (ele == null) {
            return;
        }
        log.info("handleAsyncCreateOrder.start");
        String text = ele.toString();
        POrderQueue po = JSONObject.parseObject(text, POrderQueue.class);

        try {
            PayNotifyVO notifyVO = asyncOrder(po);
            Result<Object> resp = Result.wrap(notifyVO, ResultEnum.SUCCESS);
            log.info("handleAsyncCreateOrder, p order queue: {}, call to payer req body: {}", po, resp);
            HttpRequest.post(po.getNotify())
                    .body(JSONObject.toJSONString(resp))
                    .execute();
            log.info("handleAsyncCreateOrder send payer success, pa info : {}", po.getPa());
        } catch (ConnectException ex) {
            log.error("[call back to payer error]付方回调失败,回调信息: {}, ex: {}", po, ex.getMessage());
        } catch (Exception e) {
            log.error("err", e);
            Result<Object> resp = Result.wrap("订单创建失败，请联系管理员", ResultEnum.SERVICE_ERROR);
            HttpRequest.post(po.getNotify())
                    .body(JSONObject.toJSONString(resp))
                    .execute();
            log.error("handleAsyncCreateOrder, param: {}, call to payer: {}", po, resp);
        }
        log.info("handleAsyncCreateOrder.end");
    }

    private PayNotifyVO asyncOrder(POrderQueue po) throws Exception {
        String pa = po.getPa();
        Integer channel = po.getChannel();
        String orderId = po.getOrderId();
        Integer reqMoney = po.getReqMoney();
        String notify = po.getNotify();
        String attach = po.getAttach();

        LocalDateTime nowTime = LocalDateTime.now();
        String now = DateUtil.format(nowTime, "yyyy-MM-dd");
        // 拿到所有 channel account
        List<CAccountInfo> cAccountList = cAccountMapper.listCanPayForCAccount();
        List<CAccountInfo> cAccountListToday = cAccountMapper.listCanPayForCAccountToday(now);
        for (CAccountInfo c : cAccountListToday) {
            c.setCreateTime(nowTime);
        }

        // 计算可用
        List<CAccountInfo> randomTemp = compute(channel, now, cAccountList, cAccountListToday);

        if (randomTemp.size() == 0) {
            // 告知付方系统无可用充值账户，请联系管理员
            Result<Object> resp = Result.wrap("系统无可用充值账户，请联系管理员", ResultEnum.SERVICE_ERROR);
            HttpRequest.post(notify)
                    .body(JSONObject.toJSONString(resp))
                    .execute();
            log.info("handleAsyncCreateOrder, param: {}, call to payer: {}", po, resp);
            throw new NotFoundException("系统无可用充值账户，请联系管理员");
        }

        int randomIndex = RandomUtil.randomInt(randomTemp.size());
        CAccountInfo randomACInfo = randomTemp.get(randomIndex);

        PayInfo payInfo = new PayInfo();
        CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(randomACInfo.getCid(), randomACInfo.getGid());
        payInfo.setChannel(cgi.getCChannel());
        payInfo.setRepeat_passport(randomACInfo.getAcAccount());
        payInfo.setGame(cgi.getCGame());
        payInfo.setGateway(cgi.getCGateway());
        payInfo.setRecharge_unit(reqMoney);
        payInfo.setRecharge_type(6);
        payInfo.setCk(randomACInfo.getCk());

        JSONObject orderResp = gee4Service.createOrder(payInfo);
        if (orderResp == null || orderResp.get("data") == null) {
            log.error("create order error, resp -> {}", orderResp);
            // 告知付方订单创建失败，请联系管理员
            Result<Object> resp = Result.wrap("订单创建失败，请联系管理员", ResultEnum.SERVICE_ERROR);
            HttpRequest.post(notify)
                    .body(JSONObject.toJSONString(resp))
                    .execute();
            log.error("handleAsyncCreateOrder, param: {}, call to payer: {}", po, resp);
            throw new ServiceException("订单创建失败，请联系管理员");
        }
        if (orderResp.getInteger("code") != 1) {
            log.error("create order error, resp -> {}", orderResp);
            // 告知付方订单创建失败，请联系管理员
            Result<Object> resp = Result.wrap("订单创建失败，请联系管理员", ResultEnum.SERVICE_ERROR);
            HttpRequest.post(notify)
                    .body(JSONObject.toJSONString(resp))
                    .execute();
            log.error("handleAsyncCreateOrder, param: {}, call to payer: {}", po, resp);
            throw new ServiceException(orderResp.toString());
        }
        // --- 入库

        JSONObject data = orderResp.getJSONObject("data");
        String platform_oid = data.getString("vouch_code");
        String resource_url = data.getString("resource_url");

        String acId = randomACInfo.getAcid();
        String cChannelId = cgi.getCChannelId();

        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setPAccount(pa);
        payOrder.setCost(reqMoney);
        payOrder.setAcId(acId);
        payOrder.setPlatformOid(platform_oid);
        payOrder.setCChannelId(cChannelId);
        payOrder.setResourceUrl(resource_url);
        payOrder.setOrderStatus(OrderStatusEnum.NO_PAY.getCode());
        payOrder.setCallbackStatus(OrderCallbackEnum.NOT_CALLBACK.getCode());
        payOrder.setCreateTime(nowTime);
        pOrderMapper.insert(payOrder);

        PayOrderEvent event = new PayOrderEvent();
        event.setOrderId(orderId);
        event.setEventLog(data.toJSONString());
        event.setPlatformOid(platform_oid);
        event.setCreateTime(nowTime);
        pOrderEventMapper.insert(event);

        DelayTask<PayOrder> delayTask = new DelayTask<>();
        delayTask.setId(IdUtil.randomUUID());
        delayTask.setTaskName("order_delay_" + platform_oid);
        delayTask.setTask(payOrder);

        // 5min 后未支付，设置超时
        boolean delaySetting = redisUtil.zAdd(CommonConstant.ORDER_DELAY_QUEUE, delayTask, 300000); //1000ms - 5min超时
        if (delaySetting) {
            log.info("delay info: {}, expire time: {}", delayTask, 20000);
        }

        PayNotifyVO payNotifyVO = new PayNotifyVO();
        payNotifyVO.setOrder_id(orderId);
        payNotifyVO.setCost(reqMoney);

        return payNotifyVO;
    }

    @NotNull
    private List<CAccountInfo> compute(Integer channelId, String now, List<CAccountInfo> cAccountList, List<CAccountInfo> cAccountListToday) throws IOException {
        cAccountList.addAll(cAccountListToday);
        Map<String, Map<String, List<CAccountInfo>>> collect = cAccountList.stream()
                .collect(
                        Collectors.groupingBy(c -> {
                                    return c.getId() + "." + c.getCaid();
                                }
                                , Collectors.groupingBy(cc -> {
                                    LocalDateTime createTime = cc.getCreateTime();
                                    if (createTime == null) {
                                        return "null";
                                    }
                                    String date = DateUtil.format(createTime, "yyyy-MM-dd");
                                    return date;
                                })
                        )
                );

        // 日求和
        Map<String, Integer> dailySum = cAccountList.stream()
                .collect(
                        Collectors.groupingBy(c -> {
                                    LocalDateTime createTime = c.getCreateTime();
                                    String date = DateUtil.format(createTime, "yyyy-MM-dd");

                                    return c.getId() + "." + c.getCaid() + "|" + date;
                                }
                                , Collectors.summingInt(e -> {
                                    if (e.getCost() == null) {
                                        return 0;
                                    } else return e.getCost();
                                })
                        )
                );

        // 总求和
        Map<String, Integer> totalSum = cAccountList.stream()
                .collect(
                        Collectors.groupingBy(c -> c.getId() + "." + c.getCaid()
                                , Collectors.summingInt(e -> {
                                    if (e.getCost() == null) {
                                        return 0;
                                    } else return e.getCost();
                                })
                        )
                );

        // 判断求和大于总限额的先过滤
        Iterator<Map.Entry<String, Map<String, List<CAccountInfo>>>> it = collect.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map<String, List<CAccountInfo>>> entry = it.next();
            String key = entry.getKey();
            Map<String, List<CAccountInfo>> valueMap = entry.getValue();

            Integer totalCost = totalSum.get(key);
            CAccountInfo temp = null;
            for (String k : valueMap.keySet()) {
                List<CAccountInfo> cl = valueMap.get(k);
                temp = cl.get(0);
                break;
            }
            if (temp != null && totalCost >= temp.getTotalLimit()) it.remove();
        }

        // 日限额过滤
        Iterator<Map.Entry<String, Map<String, List<CAccountInfo>>>> it2 = collect.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<String, Map<String, List<CAccountInfo>>> entry = it2.next();
            String key = entry.getKey();
            Map<String, List<CAccountInfo>> valueMap = entry.getValue();

            Iterator<Map.Entry<String, List<CAccountInfo>>> it_inner = valueMap.entrySet().iterator();
            while (it_inner.hasNext()) {
                Map.Entry<String, List<CAccountInfo>> next = it_inner.next();
                List<CAccountInfo> cValue = next.getValue();
                Iterator<CAccountInfo> itrList = cValue.iterator();
                while (itrList.hasNext()) {
                    CAccountInfo c = itrList.next();
                    LocalDateTime createTime = c.getCreateTime();
                    if (createTime == null) continue;
                    String date = DateUtil.format(createTime, "yyyy-MM-dd");
                    if (!now.equals(date)) {
                        itrList.remove();
                        continue;
                    }

                    Integer dailyCost = dailySum.get(key + "|" + now);
                    Integer dailyLimit = c.getDailyLimit();
                    if (dailyLimit == null || dailyLimit == 0) continue;
                    if (dailyCost >= dailyLimit) {
                        itrList.remove();
                    }
                }

                if (cValue.size() == 0) it_inner.remove();
            }

            if (valueMap.values().size() == 0) {
                it2.remove();
            }
        }

        // 计算一个可用的 account
        ArrayList<Map<String, List<CAccountInfo>>> randomList = new ArrayList<>(collect.values());

        Set<CAccountInfo> randomSet = new HashSet<>();
        for (Map<String, List<CAccountInfo>> valMap : randomList) {
            for (List<CAccountInfo> valList : valMap.values()) {
                randomSet.addAll(valList);
            }
        }

        List<CAccountInfo> randomTemp = new ArrayList<>(randomSet);

        //随机打乱
        Collections.shuffle(randomTemp);

        Iterator<CAccountInfo> iterator = randomTemp.iterator();
        List<CAccountInfo> rs = new ArrayList<>();
        while (iterator.hasNext()) {
            CAccountInfo c = iterator.next();
            if (Objects.equals(channelId, c.getCid())) {
                // --------------- --------------
                // 总账户充值
                Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(c.getUid());

                // 总订单充值（花费）
                Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(c.getUid());

                totalRecharge = totalRecharge == null ? 0 : totalRecharge;
                totalCost = totalCost == null ? 0 : totalCost;
                // 总余额
                int balance = totalRecharge - totalCost;
                if (balance <= 0) {
                    CAccount cAccount = new CAccount();
                    cAccount.setId(c.getId());
                    cAccount.setSysStatus(0);
                    cAccount.setSysLog("总余额不足，请联系管理员充值");
                    cAccountMapper.updateById(cAccount);
                    iterator.remove();
                    continue;
                }

                // 获取ck
                String acAccount = c.getAcAccount();
                String acPwd = Base64.decodeStr(c.getAcPwd());

                String cookie = payService.getCK(acAccount, acPwd);

                c.setCk(cookie);
                String ck = c.getCk();
                boolean expire = gee4Service.tokenCheck(ck, acAccount);
                if (!expire) {
                    CAccount cAccount = new CAccount();
                    cAccount.setId(c.getId());
                    cAccount.setSysStatus(0);
                    cAccount.setSysLog("充值账户或密码有误，请更新确认");
                    cAccountMapper.updateById(cAccount);
                    iterator.remove();
                    continue;
                }

                rs.add(c);
                break;
            }
        }
        return rs;
    }
}
