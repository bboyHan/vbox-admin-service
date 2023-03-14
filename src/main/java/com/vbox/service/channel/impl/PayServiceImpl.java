package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.common.ResultOfList;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.*;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.PayerInfoThreadHolder;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.*;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.*;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import com.vbox.service.task.DelayTask;
import com.vbox.service.task.Gee4Service;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PayServiceImpl extends ServiceImpl<PAccountMapper, PAccount> implements PayService {

    @Autowired
    private PAccountMapper pAccountMapper;
    @Autowired
    private PAuthMapper pAuthMapper;
    @Autowired
    private RelationURMapper urMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private CAccountMapper cAccountMapper;
    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private CGatewayMapper cGatewayMapper;
    @Autowired
    private Gee4Service gee4Service;
    @Autowired
    private POrderMapper pOrderMapper;
    @Autowired
    private POrderEventMapper pOrderEventMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private RelationUSMapper relationUSMapper;
    @Autowired
    private CAccountWalletMapper cAccountWalletMapper;
    @Autowired
    private VboxUserWalletMapper vboxUserWalletMapper;

    @Override
    public int createPAccount(PAccountParam param) {

        //1. p account info
        PAccount pAccount = new PAccount();
        pAccount.setPAccount(IdUtil.fastSimpleUUID());
        pAccount.setPKey(IdUtil.fastSimpleUUID());
        pAccount.setPRemark(param.getP_remark());
        pAccount.setStatus(param.getStatus());
        pAccount.setCreateTime(LocalDateTime.now());
        save(pAccount);

        //2. p account auth
        PAuth pAuth = new PAuth();
        KeyPair rsa = SecureUtil.generateKeyPair("RSA");
        pAuth.setPid(pAccount.getId());
        pAuth.setSecret(Base64.encode(rsa.getPrivate().getEncoded()));
        pAuth.setPub(Base64.encode(rsa.getPublic().getEncoded()));
        pAuth.setCreateTime(LocalDateTime.now());
        pAuthMapper.insert(pAuth);

        return 0;
    }


    @Override
    public Object createOrder(OrderCreateParam orderCreateParam) throws Exception {
        String pa = orderCreateParam.getP_account();
        String orderId = orderCreateParam.getP_order_id();
        Channel channel = paramCheckCreateOrder(orderCreateParam);
        Integer reqMoney = orderCreateParam.getMoney();
        LocalDateTime nowTime = LocalDateTime.now();
        CAccountInfo randomACInfo = new CAccountInfo();
        String account;
        String now;
        if (null == orderCreateParam.getAcid()) {
            List<CAccountInfo> cAccountList = this.cAccountMapper.listCanPayForCAccount();
            if (cAccountList == null || cAccountList.size() == 0) {
                throw new NotFoundException("系统不可用充值渠道，请联系管理员");
            }

            String orderKey = "redisLock_order:" + orderId;
            if (redisUtil.hasKey(orderKey)) {
                throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
            }

            this.redisUtil.set(orderKey, 1, 300L);
            log.info("create order 创建订单: {}, p account: {}", orderId, pa);
            redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】正在创建订单....  ");
            reqMoney = orderCreateParam.getMoney();
            nowTime = LocalDateTime.now();
            now = DateUtil.format(nowTime, "yyyy-MM-dd");
            List<CAccountInfo> cAccountListToday = cAccountMapper.listCanPayForCAccountToday(now);
            for (CAccountInfo c : cAccountListToday) {
                c.setCreateTime(nowTime);
            }

            List<CAccountInfo> randomTemp = this.compute(channel.getId(), now, cAccountList, cAccountListToday);
            if (randomTemp.size() == 0) {
                throw new NotFoundException("系统无可用充值账户，请联系管理员");
            }

            int randomIndex = RandomUtil.randomInt(randomTemp.size());
            randomACInfo = randomTemp.get(randomIndex);
        } else {
            CAccount acDB = cAccountMapper.selectOne((new QueryWrapper<CAccount>()).eq("acid", orderCreateParam.getAcid()));
            if (acDB.getStatus() != 1 || acDB.getSysStatus() != 1) {
                throw new ServiceException("该账户未开启后台设置开关，不允许建单");
            }

            BeanUtils.copyProperties(acDB, randomACInfo);
        }

        PayInfo payInfo = new PayInfo();
        CGatewayInfo cgi = this.cGatewayMapper.getGateWayInfoByCIdAndGId(randomACInfo.getCid(), randomACInfo.getGid());
//        payInfo.setChannel(cgi.getCChannel());
        payInfo.setChannel("weixin_mobile");
        account = randomACInfo.getAcAccount();
        payInfo.setRepeat_passport(account);
        payInfo.setGame(cgi.getCGame());
        payInfo.setGateway(cgi.getCGateway());
        payInfo.setRecharge_unit(reqMoney);
        payInfo.setRecharge_type(6);
        String acPwd = randomACInfo.getAcPwd();
        String cookie = this.getCK(account, Base64.decodeStr(acPwd));
        boolean expire = this.gee4Service.tokenCheck(cookie, account);
        if (!expire) {
            //TODO
            redisUtil.del("account:ck:" + account);
            cookie = this.getCK(account, Base64.decodeStr(acPwd));
            expire = this.gee4Service.tokenCheck(cookie, account);
            if (!expire) {
                throw new NotFoundException("ck问题，请联系管理员");
            }
        }

        payInfo.setCk(cookie);
        redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】正在创建订单.... ck 校验成功  ");
        log.info("【商户：" + pa + "】【订单ID：" + orderId + "】正在创建订单.... ck 校验成功  ");
        JSONObject orderResp = gee4Service.createOrder(payInfo);
        if (orderResp != null && orderResp.get("data") != null) {
            if (orderResp.getInteger("code") != 1) {
                throw new ServiceException(orderResp.toString());
            } else {
                JSONObject data = orderResp.getJSONObject("data");
                String platform_oid = data.getString("vouch_code");
                String resource_url = data.getString("resource_url");

                String payUrl = handelPayUrl(data, resource_url);
                String acId = randomACInfo.getAcid();
                String cChannelId = cgi.getCChannelId();
                PayOrder payOrder = new PayOrder();
                payOrder.setOrderId(orderId);
                payOrder.setPAccount(pa);
                payOrder.setCost(reqMoney);
                payOrder.setAcId(acId);
                payOrder.setPlatformOid(platform_oid);
                payOrder.setCChannelId(cChannelId);
                String h5Url = "http://mng.vboxjjjxxx.info/#/code/pay?orderId=" + orderId;
                payOrder.setResourceUrl(payUrl);
                payOrder.setNotifyUrl(orderCreateParam.getNotify_url());
                payOrder.setOrderStatus(OrderStatusEnum.NO_PAY.getCode());
                payOrder.setCallbackStatus(OrderCallbackEnum.NOT_CALLBACK.getCode());
                payOrder.setCodeUseStatus(CodeUseStatusEnum.FINISHED.getCode());
                payOrder.setCreateTime(nowTime);
                pOrderMapper.insert(payOrder);

                PayOrderEvent event = new PayOrderEvent();
                event.setOrderId(orderId);
                event.setEventLog(data.toJSONString());
                event.setPlatformOid(platform_oid);
                event.setCreateTime(nowTime);
                if ("weixin_mobile".equalsIgnoreCase(data.getString("channel"))) {
                    URL url = URLUtil.url(resource_url);
                    Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), null);
                    String redirect_url = URLDecoder.decode(stringMap.get("redirect_url"), "utf-8");
                    stringMap.put("redirect_url", redirect_url);
                    Map<String, Object> objectObjectSortedMap = new HashMap<>(stringMap);
                    HttpResponse execute = HttpRequest.post("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb")
                            .form(objectObjectSortedMap)
                            .contentType("application/x-www-form-urlencoded")
                            .header("X-Requested-With", "com.seasun.gamemgr")
                            .header("Origin", "https://m.xoyo.com")
                            .header("Referer", "https://m.xoyo.com")
                            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                            .execute();
                    String body = execute.body();
                    event.setExt(body);
                }
                pOrderEventMapper.insert(event);

                DelayTask<PayOrder> delayTask = new DelayTask();
                delayTask.setId(IdUtil.randomUUID());
                delayTask.setTaskName("order_delay_" + platform_oid);
                delayTask.setTask(payOrder);
                boolean delaySetting = redisUtil.zAdd("order_delay_queue", delayTask, 300000L);
                if (delaySetting) {
                    log.info("delay info: {}, expire time: {}", delayTask, 20000);
                }

                PayOrderCreateVO p = new PayOrderCreateVO();
                p.setPayUrl(h5Url);
                p.setOrderId(orderId);
                p.setCost(reqMoney);
                p.setAttach(orderCreateParam.getAttach());
                p.setStatus(2);
                p.setChannelId(orderCreateParam.getChannel_id());
                redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】创建订单完成.... 付款链接: " + h5Url);
                log.info("【商户：" + pa + "】【订单ID：" + orderId + "】创建订单完成.... 付款链接: " + h5Url);
                return p;
            }
        } else {
            log.error("create order error, resp -> {}", orderResp);
            throw new ServiceException("订单创建失败，请联系管理员");
        }
    }

    private String handelPayUrl(JSONObject data, String resource_url) throws UnsupportedEncodingException {
        String payUrl = "";
        if ("wyzxpoto".equalsIgnoreCase(data.getString("channel"))) {
            URL url = URLUtil.url(resource_url);
            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
            Map<String, Object> objectObjectSortedMap = new HashMap(stringMap);
            HttpResponse execute = HttpRequest.post("https://wepay.jd.com/jdpay/saveOrder").setFollowRedirects(false).form(objectObjectSortedMap).execute();
            String location = execute.header("Location");
            log.info("location: {}", location);
            URL redirect = URLUtil.url("http://127.0.0.1" + location);
            Map<String, String> rMap = HttpUtil.decodeParamMap(redirect.getQuery(), Charset.defaultCharset());
            String tradeNum = rMap.get("tradeNum");
            String key = rMap.get("key");
            String ourTradeNum = rMap.get("ourTradeNum");
            String jdPay = "https://wepay.jd.com/jdpay/payIndex?tradeNum=" + tradeNum + "&orderId=" + ourTradeNum + "&key=" + key;
            log.info("jd pay url: {}", jdPay);
            if (tradeNum == null) {
                jdPay = "https://wepay.jd.com/jdpay/login?key=" + key;
            }

            log.info(" 修正 后 pay url: {}", jdPay);
            payUrl = jdPay;
        }

        if ("alipay_qr".equalsIgnoreCase(data.getString("channel"))) {
            payUrl = resource_url;
        }

        if ("weixin".equalsIgnoreCase(data.getString("channel"))) {
            payUrl = resource_url;
        }
        if ("weixin_mobile".equalsIgnoreCase(data.getString("channel"))) {
//            URL url = URLUtil.url(resource_url);
//            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), null);
//            String redirect_url = URLDecoder.decode(stringMap.get("redirect_url"), "utf-8");
//            stringMap.put("redirect_url", redirect_url);
//            Map<String, Object> objectObjectSortedMap = new HashMap(stringMap);
//            HttpResponse execute = HttpRequest.post("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb")
//                    .form(objectObjectSortedMap)
//                    .contentType("application/x-www-form-urlencoded")
//                    .header("X-Requested-With", "com.seasun.gamemgr")
//                    .header("Origin", "https://m.xoyo.com")
//                    .header("Referer", "https://m.xoyo.com")
//                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                    .execute();
//            String body = execute.body();
            payUrl = resource_url;
        }else {
            payUrl = resource_url;
        }

        return payUrl;
    }

    public Channel paramCheckCreateOrder(OrderCreateParam orderCreateParam) throws IllegalAccessException {
        String pa = orderCreateParam.getP_account();
        String sign = orderCreateParam.getSign();
        PAccount paDB = pAccountMapper.selectOne((new QueryWrapper<PAccount>()).eq("p_account", pa));
        String pKey = paDB.getPKey();
        orderCreateParam.setSign((String)null);
        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);
        String signDB = CommonUtil.encodeSign(map, pKey);
        if (!signDB.equals(sign)) {
            throw new ValidateException("入参仅限文档包含字段，请核对");
        } else {
            String channelId = orderCreateParam.getChannel_id();
            Channel channel = channelMapper.getChannelByChannelId(channelId);
            if (channel == null) {
                throw new ValidateException("通道id错误，请重新查询确认");
            } else {
                String notify = orderCreateParam.getNotify_url();
                boolean isUrl = CommonUtil.isUrl(notify);
                if (!isUrl) {
                    throw new ValidateException("notify_url不合法，请检验入参");
                } else {
                    String attach = orderCreateParam.getAttach();
                    if (attach != null && attach.length() > 128) {
                        throw new ValidateException("attach不合法，请检验入参");
                    } else {
                        String orderId = orderCreateParam.getP_order_id();
                        if (orderId != null && orderId.length() <= 32 && orderId.length() >= 16) {
                            PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
                            if (poDB != null) {
                                throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
                            } else {
                                return channel;
                            }
                        } else {
                            throw new ValidateException("orderId不合法，请检验入参");
                        }
                    }
                }
            }
        }
    }

    @Override
    public Object createTestOrder(Integer num, String acid, String channel) throws Exception {
        OrderCreateParam orderCreateParam = new OrderCreateParam();
        String orderId = IdUtil.simpleUUID();
        orderCreateParam.setP_order_id(orderId);
        orderCreateParam.setMoney(num);
        orderCreateParam.setNotify_url("http://mng.vboxjjjxxx.info/basic-api/test/callback");
        orderCreateParam.setChannel_id(channel);
        orderCreateParam.setAcid(acid);
        orderCreateParam.setP_account("e191aa33c9a74416b6ae6aa66d7195f1");
        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);
        String sign = CommonUtil.encodeSign(map, "00b79aa26d6f412984c8926300427e39");
        orderCreateParam.setSign(sign);
        return createOrder(orderCreateParam);
    }

//    @Override
//    public Object createAsyncOrder(OrderCreateParam orderCreateParam) throws Exception {
//
//        // 1. 校验商户
//        PayerInfo payerLocal = PayerInfoThreadHolder.getPayerInfo();
//        String pa = orderCreateParam.getP_account();
//        String pub = orderCreateParam.getP_key();
//        boolean valid = PayerInfo.valid(payerLocal, new PayerInfo(pub, pa));
//        if (!valid) throw new ValidateException("Token valid");
//
//        // 2. param check
//        String channelId = orderCreateParam.getChannel_id();
//        Channel channel = channelMapper.getChannelByChannelId(channelId);
//        if (channel == null) throw new ValidateException("通道id错误，请重新查询确认");
//        String notify = orderCreateParam.getNotify_url();
//        boolean isUrl = CommonUtil.isUrl(notify);
//        if (!isUrl) throw new ValidateException("notify_url不合法，请检验入参");
//
//        String attach = orderCreateParam.getAttach();
//        if (attach != null && attach.length() > 128) throw new ValidateException("attach不合法，请检验入参");
//        String orderId = orderCreateParam.getP_order_id();
//        if (orderId == null || orderId.length() > 32 || orderId.length() < 16)
//            throw new ValidateException("orderId不合法，请检验入参");
//        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
//        if (poDB != null) throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
//
//        List<CAccountInfo> cAccountList = cAccountMapper.listCanPayForCAccount();
//        if (cAccountList == null || cAccountList.size() == 0) {
//            throw new NotFoundException("系统不可用充值渠道，请联系管理员");
//        }
//
//        String orderKey = "redisLock_order:" + orderId;
//        if (redisUtil.hasKey(orderKey)) {
//            throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
//        }
//        redisUtil.set(orderKey, 1, 300000);
////        RLock lock = redissonClient.getLock(orderKey);
////        try {
////            lock.lock(30, TimeUnit.SECONDS);
////            if (!tryLock) throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
//        log.info("lock create order: {}, p account: {}", orderId, pa);
//
//        // 3. 根据付方所需金额，列出可充值资源池
//        Integer reqMoney = orderCreateParam.getMoney();
//
//        POrderQueue pOrderQueue = new POrderQueue();
//        pOrderQueue.setPa(payerLocal.getAccount());
//        pOrderQueue.setChannel(channel.getId());
//        pOrderQueue.setOrderId(orderId);
//        pOrderQueue.setReqMoney(reqMoney);
//        pOrderQueue.setNotify(notify);
//        pOrderQueue.setPayType(1);
//        pOrderQueue.setAttach(attach);
//        redisUtil.lPush(CommonConstant.ORDER_CREATE_QUEUE, pOrderQueue);
//
//        return pOrderQueue;
////        }
////        finally {
////            try{
////                if (lock.isLocked()) {
////                    lock.unlock();
////                }
////            }catch (IllegalMonitorStateException ex){
////                log.info("unlock [normal] create order: {}, p account: {}, ex: {}", orderId, pa, ex.getStackTrace()[0]);
////            }
////        }
//    }

    @NotNull
    private PayOrder asyncOrder(PayerInfo payerLocal, Channel channel, String orderId, Integer reqMoney) throws Exception {
        LocalDateTime nowTime = LocalDateTime.now();
        String now = DateUtil.format(nowTime, "yyyy-MM-dd");
        // 拿到所有 channel account
        List<CAccountInfo> cAccountList = cAccountMapper.listCanPayForCAccount();


        List<CAccountInfo> cAccountListToday = cAccountMapper.listCanPayForCAccountToday(now);
        for (CAccountInfo c : cAccountListToday) {
            c.setCreateTime(nowTime);
        }
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

        List<CAccountInfo> randomTemp = new ArrayList<>();
        for (Map<String, List<CAccountInfo>> valMap : randomList) {
            for (List<CAccountInfo> valList : valMap.values()) {
                randomTemp.addAll(valList);
            }
        }

        Iterator<CAccountInfo> iterator = randomTemp.iterator();
        while (iterator.hasNext()) {
            CAccountInfo c = iterator.next();
            if (Objects.equals(channel.getId(), c.getCid())) {
                String ck = c.getCk();
                boolean expire = gee4Service.tokenCheck(ck, c.getAcAccount());
                if (!expire) {
                    CAccount cAccount = new CAccount();
                    cAccount.setId(c.getId());
                    cAccount.setSysStatus(0);
                    cAccount.setSysLog("ck已过期，请及时更新");
                    cAccountMapper.updateById(cAccount);
                    iterator.remove();
                }
            }
        }
        if (randomTemp.size() == 0) throw new NotFoundException("系统无可用充值账户，请联系管理员");

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
            throw new ServiceException("订单创建失败，请联系管理员");
        }
        if (orderResp.getInteger("code") != 1) {
            throw new ServiceException(orderResp.toString());
        }
        // --- 入库

        JSONObject data = orderResp.getJSONObject("data");
        String platform_oid = data.getString("vouch_code");
        String resource_url = data.getString("resource_url");

        String pAccount = payerLocal.getAccount();
        String acId = randomACInfo.getAcid();
        String cChannelId = cgi.getCChannelId();

        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setPAccount(pAccount);
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

        // 1min 后未支付，设置超时
        boolean delaySetting = redisUtil.zAdd(CommonConstant.ORDER_DELAY_QUEUE, delayTask, 300000); //1000ms
        if (delaySetting) {
            log.info("delay info: {}, expire time: {}", delayTask, 20000);
        }
        return payOrder;
    }


    @Override
    public ResultOfList<List<PAccountVO>> listPAccount() throws Exception {

        // super check
        Integer id = TokenInfoThreadHolder.getToken().getId();
        List<Integer> ridList = urMapper.getRidByUid(id);
        if (ridList == null || ridList.size() == 0) throw new Exception("no auth");
        for (Integer rid : ridList) {
            Role role = roleMapper.selectById(rid);
            if (role != null && !role.getRoleValue().equalsIgnoreCase("super_admin")) {
                throw new Exception("no auth");
            }
        }

        List<PAccountVO> rsList = pAccountMapper.listPAccountInfo();

        ResultOfList<List<PAccountVO>> rl = new ResultOfList<>(rsList, rsList.size());
        return rl;
    }

    @Override
    public int delPAccount(Integer pid) {

        int i1 = pAuthMapper.deleteByPid(pid);
        int i2 = pAccountMapper.deleteById(pid);
        return i1 + i2;
    }

    @Override
    public int updPAccount(Integer pid, PAccountParam param) throws Exception {
        PAccount p = pAccountMapper.selectById(pid);
        if (p == null) throw new Exception("pc not exist!");

        PAccount upd = new PAccount();
        upd.setId(p.getId());
        upd.setPRemark(param.getP_remark() == null ? null : param.getP_remark());

        int i = pAccountMapper.updateById(upd);
        return i;
    }

    @Override
    public String preAuth(OrderPreAuthParam authParam) throws Exception {
        PAccountVO p = pAccountMapper.getInfoByAccountAndPub(authParam.getP_account(), authParam.getP_key());
        if (p == null) {
            throw new Exception("user is not exist!");
        }

        String secret = p.getSecret();
        PrivateKey privateKey = SecureUtil.rsa(secret, null).getPrivateKey();

        // setting expire time
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 12);

        String token = JWT.create()
                .addHeaders(new HashMap<>())
                .setPayload("account", p.getP_account())
                .setPayload("pub", p.getPub())
                .setExpiresAt(calendar.getTime())
                .sign(JWTSignerUtil.rs256(privateKey));

        return token;
    }

    /**
     * 查询账户自己的所有订单
     */
    @Override
    public Object listOrder() {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        List<String> acIdList = cAccountMapper.listAcIdInUids(sidList);
        if (acIdList.size() == 0) return new ArrayList<>();
        QueryWrapper<PayOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("ac_id", acIdList);
        queryWrapper.orderByDesc("id");

        List<PayOrder> payOrders = pOrderMapper.selectList(queryWrapper);

        List<PayOrderVO> voList = new ArrayList<>(payOrders.size());
        for (PayOrder p : payOrders) {
            PayOrderVO target = new PayOrderVO();
            BeanUtils.copyProperties(p, target);
            target.setPa(p.getPAccount());

            CAccount ca = cAccountMapper.getCAccountByAcid(p.getAcId());
            target.setAcRemark(ca.getAcRemark());
            target.setAcAccount(ca.getAcAccount());
            target.setChannel(ChannelEnum.of(p.getCChannelId()));
            target.setCallbackStatus(p.getCallbackStatus());
            voList.add(target);
        }

        return voList;
    }

    @Override
    public long orderCallback(OrderCallbackParam callbackParam) throws Exception {
        // 1. 校验商户
        PayerInfo payerLocal = PayerInfoThreadHolder.getPayerInfo();
        String pa = callbackParam.getP_account();
        String pub = callbackParam.getP_key();
        boolean valid = PayerInfo.valid(payerLocal, new PayerInfo(pub, pa));
        if (!valid) throw new ValidateException("Token valid");

        String orderId = callbackParam.getP_order_id();
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        if (po == null) throw new NotFoundException("订单不存在");

        //
//        OrderCallbackVO vo = new OrderCallbackVO();

        Integer payStatus = callbackParam.getPay_status();
        if (payStatus == 0 || payStatus == 3) { //告知 支付失败 或者 超时
            log.warn("[notify] call back to me pay failed,支付失败的订单不允许回调通知商户, order info: {}", po);
            pOrderMapper.updateStatusByOIdWhenCall(orderId, payStatus, CodeUseStatusEnum.FAILED.getCode());
        }
        if (payStatus == 1) {
            Object order = redisUtil.sGetOne(orderId);
            if (order != null) throw new DuplicateKeyException("已通知回调支付，请勿重复操作");

            long row = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);

            return row;
//            //生产
//            JSONObject resp = queryOrder(orderId);
//            JSONObject data = resp.getJSONObject("data");
//            Integer code = data.getInteger("order_status");
//            //测试
////            Integer code = 1;
//            if (code == 0) { // 告知 支付成功，但查询平台，发现未支付，则改为支付失败 0
//                pOrderMapper.updateStatusByOIdWhenCall(orderId, OrderStatusEnum.PAY_FAILED.getCode(), CodeStatusEnum.PLATFORM_NOT_PAY.getCode());
//
//                vo.setPay_status(OrderStatusEnum.PAY_FAILED.getCode());
//                vo.setPay_desc(OrderStatusEnum.PAY_FAILED.getMsg());
//                vo.setP_order_id(orderId);
//                vo.setMsg(CodeStatusEnum.PLATFORM_NOT_PAY.getMsg());
//                return vo;
//            }
//
//            if (code == 1) { // 告知 支付成功，查询平台已支付，则改为支付成功 1
//                pOrderMapper.updateStatusByOIdWhenCall(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeStatusEnum.FINISHED.getCode());
//
//                vo.setPay_status(OrderStatusEnum.PAY_FINISHED.getCode());
//                vo.setPay_desc(OrderStatusEnum.PAY_FINISHED.getMsg());
//                vo.setMsg(CodeStatusEnum.FINISHED.getMsg());
//                vo.setP_order_id(orderId);
//
//                //支付入库 wallet
//                String acId = po.getAcId();
//                CAccount ca = cAccountMapper.selectOne(new QueryWrapper<CAccount>().eq("acid", acId));
//
//                CAccountWallet w = new CAccountWallet();
//                w.setOid(orderId);
//                w.setCaid(ca.getId());
//                w.setCreateTime(LocalDateTime.now());
//                w.setCost(po.getCost());
//                cAccountWalletMapper.insert(w);
//                return vo;
//            }
        }

        return 0;
    }

    @Override
    public OrderQueryVO queryOrderToP(OrderCreateParam orderCreateParam) throws Exception {
        String pa = orderCreateParam.getP_account();
        String sign = orderCreateParam.getSign();
        PAccount paDB = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", pa));
        String pKey = paDB.getPKey();
        orderCreateParam.setSign(null);

        String signDB = CommonUtil.encodeSign(CommonUtil.objToTreeMap(orderCreateParam), pKey);
        if (!signDB.equals(sign)) throw new ValidateException("Token valid");

        String orderId = orderCreateParam.getP_order_id();
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        if (po == null) throw new NotFoundException("订单不存在");

        JSONObject data = null;
        CAccountWallet wallet = cAccountWalletMapper.selectOne(new QueryWrapper<CAccountWallet>().eq("oid", orderId));
        Integer code;
        JSONObject resp;
        if (wallet != null) {
            code = 2;
            log.info("商户查单时发现该订单已支付入库, info: {}", wallet);
        } else {
            resp = this.queryOrder(orderId);
            data = resp.getJSONObject("data");
            code = data.getInteger("order_status");
        }

        if (po.getOrderStatus() == 3) {
            resp = this.queryOrder(orderId);
            data = resp.getJSONObject("data");
            code = data.getInteger("order_status");
            if (code == 2) {
                this.pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
            }
        }

        if (code == 2 && wallet != null && po.getCallbackStatus() == 2) {
            String notify = po.getNotifyUrl();
            PayNotifyVO vo = new PayNotifyVO();
            vo.setOrder_id(orderId);
            vo.setStatus(1);
            vo.setCost(po.getCost());
            String account = po.getPAccount();
            vo.setP_account(account);
            String signNew = CommonUtil.encodeSign(CommonUtil.objToTreeMap(vo), paDB.getPKey());
            vo.setSign(signNew);
            HttpResponse execute = null;

            try {
                String reqBody = JSONObject.toJSONString(vo);
                log.info("商户查单回调请求消息：notify：{}，req body：{}", notify, reqBody);
                execute = HttpRequest.post(notify).body(reqBody).execute();
                log.info("商户查单回调返回信息： http status： {}， resp： {}", execute.getStatus(), execute.body());
                if (execute.getStatus() == 200) {
                    this.pOrderMapper.updateCallbackStatusByOId(orderId);
                    this.redisUtil.setRemove("order_callback_queue", orderId);
                    log.info("该订单已回调成功，通知url：{}，orderID：{}", notify, orderId);
                }
            } catch (Exception var19) {
                log.error("商户查单回调失败，notify: {}, resp: {}, err: {}", notify, execute, var19);
            }
        }

        if (code == 2 && wallet == null) {
            int row = this.pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
            if (row == 1) {
                CAccount ca = this.cAccountMapper.getCAccountByAcid(po.getAcId());
                CAccountWallet w = new CAccountWallet();
                w.setCaid(ca.getId());
                w.setCost(po.getCost());
                w.setOid(po.getOrderId());
                w.setCreateTime(LocalDateTime.now());

                try {
                    this.cAccountWalletMapper.insert(w);
                } catch (Exception var18) {
                    log.warn("CAccountWallet已经入库， err: {}", var18.getMessage());
                }

                log.info("商戶主动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}， 平台数据：{}", po.getOrderId(), data);
                long rowRedis = this.redisUtil.sSet("order_callback_queue", orderId);
                if (rowRedis == 1L) {
                    log.info("商戶主动查单, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                }
            }
        }

        PayOrder pov = this.pOrderMapper.getPOrderByOid(orderId);
        OrderQueryVO vo = new OrderQueryVO();
        vo.setStatus(pov.getOrderStatus());
        vo.setPayUrl(pov.getResourceUrl());
        vo.setCost(pov.getCost());
        vo.setOrderId(pov.getOrderId());
        vo.setNotifyUrl(pov.getNotifyUrl());
        return vo;
    }

    @Override
    public JSONObject queryOrder(String orderId) throws Exception {

        PayOrderEvent poe = pOrderEventMapper.getPOrderEventByOid(orderId);
        if (poe == null) throw new NotFoundException("订单不存在");
        String pid = poe.getPlatformOid();

        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        CAccount ca = cAccountMapper.getCAccountByAcid(po.getAcId());

        String cookie = getCK(ca.getAcAccount(), Base64.decodeStr(ca.getAcPwd()));

        boolean expire = gee4Service.tokenCheck(cookie, ca.getAcAccount());
        if (!expire) {
            CAccount cAccount = new CAccount();
            cAccount.setId(ca.getId());
            cAccount.setSysStatus(0);
            cAccount.setSysLog("ck已过期，请及时更新");
            cAccountMapper.updateById(cAccount);
            throw new NotFoundException("ck过期，请联系运营更新后可查看订单");
        }

        SecCode secCode = gee4Service.verifyGeeCap();

        VOrderQueryParam param = new VOrderQueryParam();
        param.setCaptcha_id(secCode.getCaptcha_id());
        param.setLot_number(secCode.getLot_number());
        param.setPass_token(secCode.getPass_token());
        param.setGen_time(secCode.getGen_time());
        param.setCaptcha_output(secCode.getCaptcha_output());
        param.setVouch_code(pid);
        param.setToken(cookie);

        JSONObject resp = gee4Service.queryOrder(param);

        return resp;
    }

    @Override
    public String getCK(String acAccount, String acPwd) throws IOException {
        Object v = redisUtil.get(CommonConstant.ACCOUNT_CK + acAccount);
        if (v != null) {
            return v.toString();
        }
        String cookie = null;

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
//        File file = ResourceUtils.getFile("classpath:d4.js");
//        ClassPathResource classPathResource = new ClassPathResource("d4.js");
//        InputStream is = classPathResource.getInputStream();
        String property = System.getProperty("user.dir");
        String filePath = (property + File.separator + "d4.js");
        File inputFile = new File(filePath);
        InputStream is = new FileInputStream(inputFile);
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);   // 执行指定脚本
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String payload = (String) invoke.invokeFunction("test", acPwd);
                String encode = URLEncoder.encode(payload, "UTF-8");
//                System.out.println("pwd = " + encode);

                SecCode secCode = gee4Service.capSecCode();

                HttpResponse resp = HttpRequest.get("https://pf-api.xoyo.com/passport/common_api/login")
                        .form("account", acAccount)
                        .form("encrypt_method", "rsa")
                        .form("captcha_id", secCode.getCaptcha_id())
                        .form("lot_number", secCode.getLot_number())
                        .form("pass_token", secCode.getPass_token())
                        .form("gen_time", secCode.getGen_time())
                        .form("captcha_output", secCode.getCaptcha_output())
                        .form("password", encode)
//                                .form("callback", "jsonp_ef2891abd4b000")
                        .form("callback", "jsonp_" + RandomUtil.randomNumbers(14))
                        .execute();

                String jsonResp = Gee4Service.parseGeeJson(resp.body());
//                log.info(JSONObject.toJSONString(resp.headers()));
//                log.info(JSONObject.toJSONString(resp.body()));
                cookie = resp.headerList("Set-Cookie").get(0);

                JSONObject obj = JSONObject.parseObject(jsonResp);
//                System.out.println(obj);
//                System.out.println(data);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        redisUtil.set(CommonConstant.ACCOUNT_CK + acAccount, cookie, 7200); //2hour
        return cookie;
    }

    @Override
    public String testOrderCallback(String orderId) throws IllegalAccessException {
        PayOrder payOrder = pOrderMapper.getPOrderByOid(orderId);

        String pa = payOrder.getPAccount();
        PAccount paDB = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", pa));
        String pKey = paDB.getPKey();

        PayNotifyVO payNotifyVO = new PayNotifyVO();
        payNotifyVO.setOrder_id(payOrder.getOrderId());
        payNotifyVO.setCost(payOrder.getCost());
        payNotifyVO.setStatus(1);
        payNotifyVO.setP_account(pa);
        String sign = CommonUtil.encodeSign(CommonUtil.objToTreeMap(payNotifyVO), pKey);
        payNotifyVO.setSign(sign);

        String reqBody = JSONObject.toJSONString(payNotifyVO);
        log.info("测试回调商户, 回调请求消息：notify：{}，req body：{}", payOrder.getNotifyUrl(), reqBody);
        HttpResponse resp = HttpRequest.post(payOrder.getNotifyUrl())
                .body(reqBody)
                .execute();

        pOrderMapper.updateCallbackStatusByOId(orderId);

        log.info("测试回调商户，商户返回信息： http status： {}， resp： {}", resp.getStatus(), resp.body());
        return resp.body();
    }

    @Override
    public PayOrderCreateVO orderQuery(String orderId) {
        PayOrder payOrder = this.pOrderMapper.getPOrderByOid(orderId);
        if (payOrder == null) throw new NotFoundException("订单不存在，请联系商家核对");
        PayOrderCreateVO payOrderCreateVO = new PayOrderCreateVO();
        payOrderCreateVO.setPayUrl(payOrder.getResourceUrl());
        payOrderCreateVO.setCost(payOrder.getCost());
        payOrderCreateVO.setStatus(payOrder.getOrderStatus());
        payOrderCreateVO.setOrderId(payOrder.getOrderId());
        payOrderCreateVO.setChannelId(payOrder.getCChannelId());
        return payOrderCreateVO;
    }

    public OrderQueryVO queryAndCallback(String orderId) throws Exception {
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        if (po == null) {
            throw new NotFoundException("订单不存在，请核对");
        } else {
            PAccount paDB = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", po.getPAccount()));
            JSONObject data = null;
            CAccountWallet wallet = cAccountWalletMapper.selectOne((new QueryWrapper<CAccountWallet>()).eq("oid", orderId));
            Integer code;
            JSONObject resp;
            if (wallet != null) {
                code = 2;
                log.info("手动查单时发现该订单已支付入库, info: {}", wallet);
            } else {
                resp = this.queryOrder(orderId);
                data = resp.getJSONObject("data");
                code = data.getInteger("order_status");
            }

            if (po.getOrderStatus() == 3) {
                resp = this.queryOrder(orderId);
                data = resp.getJSONObject("data");
                code = data.getInteger("order_status");
                if (code == 2) {
                    this.pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                }
            }

            if (code == 2 && wallet != null) {
                String notify = po.getNotifyUrl();
                PayNotifyVO vo = new PayNotifyVO();
                vo.setOrder_id(orderId);
                vo.setStatus(po.getOrderStatus());
                vo.setCost(po.getCost());
                String account = po.getPAccount();
                vo.setP_account(account);
                String signNew = CommonUtil.encodeSign(CommonUtil.objToTreeMap(vo), paDB.getPKey());
                vo.setSign(signNew);
                HttpResponse execute = null;

                try {
                    String reqBody = JSONObject.toJSONString(vo);
                    log.info("手动回调请求消息：notify：{}，req body：{}", notify, reqBody);
                    execute = HttpRequest.post(notify).body(reqBody).execute();
                    log.info("手动回调返回信息： http status： {}， resp： {}", execute.getStatus(), execute.body());
                    if (execute.getStatus() == 200) {
                        this.pOrderMapper.updateCallbackStatusByOId(orderId);
                        this.redisUtil.setRemove("order_callback_queue", orderId);
                        log.info("该订单已回调成功，通知url：{}，orderID：{}", notify, orderId);
                    }
                } catch (Exception var14) {
                    log.error("手动回调失败，notify: {}, resp: {}, err: {}", notify, execute, var14);
                }
            }

            if (code == 2 && wallet == null) {
                int row = this.pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                if (row == 1) {
                    CAccount ca = this.cAccountMapper.getCAccountByAcid(po.getAcId());
                    CAccountWallet w = new CAccountWallet();
                    w.setCaid(ca.getId());
                    w.setCost(po.getCost());
                    w.setOid(po.getOrderId());
                    w.setCreateTime(LocalDateTime.now());

                    try {
                        this.cAccountWalletMapper.insert(w);
                    } catch (Exception var13) {
                        log.warn("已经入库了，{}", var13.getMessage());
                    }

                    log.info("系统手动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}， 平台数据：{}", po.getOrderId(), data);
                    long rowRedis = this.redisUtil.sSet("order_callback_queue", orderId);
                    if (rowRedis == 1L) {
                        log.info("商戶主动查单, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                    }
                }
            }

            PayOrder pov = this.pOrderMapper.getPOrderByOid(orderId);
            OrderQueryVO vo = new OrderQueryVO();
            vo.setStatus(pov.getOrderStatus());
            vo.setPayUrl(pov.getResourceUrl());
            vo.setCost(pov.getCost());
            vo.setOrderId(pov.getOrderId());
            vo.setNotifyUrl(pov.getNotifyUrl());
            return vo;
        }
    }

    @Override
    public String orderWxHtml(String orderId) {
        PayOrderEvent event = pOrderEventMapper.getPOrderEventByOid(orderId);
        return event.getExt();
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

                String cookie = getCK(acAccount, acPwd);

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


//        int retryCount = 0;
//        CAccountInfo randomACInfo;

//        for (; ; ) {
//            int randomIndex = RandomUtil.randomInt(randomList.size());
//
//            Map<String, List<CAccountInfo>> rMap = randomList.get(randomIndex);
//            ArrayList<String> randomMapKeyList = new ArrayList<>(rMap.keySet());
//            int randomKeyIndex = RandomUtil.randomInt(randomMapKeyList.size());
//            String randomMapKey = randomMapKeyList.get(randomKeyIndex);
//
//            List<CAccountInfo> cInfo = rMap.get(randomMapKey);
//            int rIndex = RandomUtil.randomInt(cInfo.size());
//            randomACInfo = cInfo.get(rIndex);
//            if (Objects.equals(channel.getId(), randomACInfo.getCid())) {
//                String ck = randomACInfo.getCk();
//                boolean expire = gee4Service.tokenCheck(ck);
//                if (!expire) {
//                    CAccount cAccount = new CAccount();
//                    cAccount.setId(randomACInfo.getId());
//                    cAccount.setSysStatus(0);
//                    cAccount.setSysLog("ck已过期，请及时更新");
//                    cAccountMapper.updateById(cAccount);
//                }
//                break;
//            }
//            retryCount++;
//            if (retryCount > 10) {
//                throw new NotFoundException("该通道帐号资源未找到，请联系管理员确认");
//            }
//        }

// 4. 生成支付链接
// String payChannel = PayTypeEnum.of(orderCreateParam.getPayType());
