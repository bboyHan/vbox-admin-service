package com.vbox.service.task;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.OrderStatusEnum;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.PayOrder;
import com.vbox.persistent.entity.PayOrderEvent;
import com.vbox.persistent.pojo.dto.CAccountInfo;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.dto.POrderQueue;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

//@Component
@Slf4j
public class OrderCreateTask {

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

    @Scheduled(cron = "0/1 * * * * ?")   //每 2s 执行一次, 接受订单创建的队列
    @Async("scheduleExecutor")
    public void handleAsyncCreateOrder2() throws Exception {
        Object ele = redisUtil.rPop(CommonConstant.ORDER_CREATE_QUEUE);
        if (ele == null) {
            return;
        }
//        Thread.sleep(200L);
        log.info("handleAsyncCreateOrder.start");

        String text = ele.toString();
        POrderQueue po = null;
        try {
            po = JSONObject.parseObject(text, POrderQueue.class);
        } catch (Exception e) {
            log.error("pOrderQueue解析异常, text: {}", text);
            return;
        }

        try {
            asyncOrder(po);
        } catch (IORuntimeException e) {
            log.error("【任务执行】IO ex 1次 : {}", e.getMessage());
            try {
                asyncOrder(po);
            } catch (IORuntimeException ex) {
                log.error("【任务执行】IO ex 2次 : {}", e.getMessage());
                asyncOrder(po);
            }
        } catch (Exception e) {
            log.error("【任务执行】handleAsyncCreateOrder, err: {}", e.getMessage());
            // 判断订单创建时间，小于2分钟的丢回队列
//            PayOrder poDB = pOrderMapper.getPOrderByOid(po.getOrderId());
//            LocalDateTime createTime = poDB.getCreateTime();
//            LocalDateTime nowTime = LocalDateTime.now().plusMinutes(-2);
//            if (nowTime.isAfter(createTime)) {
//                log.error("【任务执行】超时2分钟直接丢弃, {}", po);
//                pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
//                log.error("【任务执行】数据库置为异常单, orderId : {}", po.getOrderId());
//            } else {
//            boolean b = redisUtil.lPush(CommonConstant.ORDER_CREATE_QUEUE, po);
            log.error("【任务执行】异常单，丢弃, {}", po);
//            }
        }
        log.info("handleAsyncCreateOrder.end");
    }

    @Scheduled(cron = "0/1 * * * * ?")   //每 2s 执行一次, 接受订单创建的队列
    @Async("scheduleExecutor")
    public void handleAsyncCreateOrder() throws Exception {
        Object ele = redisUtil.rPop(CommonConstant.ORDER_CREATE_QUEUE);
        if (ele == null) {
            return;
        }
//        Thread.sleep(200L);
        log.info("handleAsyncCreateOrder.start");

        String text = ele.toString();
        POrderQueue po = null;
        try {
            po = JSONObject.parseObject(text, POrderQueue.class);
        } catch (Exception e) {
            log.error("pOrderQueue解析异常, text: {}", text);
            return;
        }

        try {
            asyncOrder(po);
        } catch (IORuntimeException e) {
            log.error("【任务执行】IO ex 1次 : {}", e.getMessage());
            try {
                asyncOrder(po);
            } catch (IORuntimeException ex) {
                log.error("【任务执行】IO ex 2次 : {}", e.getMessage());
                asyncOrder(po);
            }
        } catch (Exception e) {
            log.error("【任务执行】handleAsyncCreateOrder, err: {}", e.getMessage());
            // 判断订单创建时间，小于2分钟的丢回队列
//            PayOrder poDB = pOrderMapper.getPOrderByOid(po.getOrderId());
//            LocalDateTime createTime = poDB.getCreateTime();
//            LocalDateTime nowTime = LocalDateTime.now().plusMinutes(-2);
//            if (nowTime.isAfter(createTime)) {
//                log.error("【任务执行】超时2分钟直接丢弃, {}", po);
//                pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
//                log.error("【任务执行】数据库置为异常单, orderId : {}", po.getOrderId());
//            } else {
//                boolean b = redisUtil.lPush(CommonConstant.ORDER_CREATE_QUEUE, po);
//                log.error("【任务执行】重新丢回队列, {}, push: {}", po, b);
//            }
            log.error("【任务执行】异常单，丢弃, {}", po);
        }
        log.info("handleAsyncCreateOrder.end");
    }

    private void asyncOrder(POrderQueue po) throws Exception {
        String pa = po.getPa();
        String orderId = po.getOrderId();
        String payIp = po.getPayIp();
        Integer reqMoney = po.getReqMoney();
        String pr = po.getPr();
        String area = po.getArea();
        String channelId = po.getChannelId();
        Integer cid = po.getChannel();

        String account;
        CAccount c = null;

        Object ele = redisUtil.rPop(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid);
        if (ele == null) {
            List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                    .eq("status", 1)
                    .eq("sys_status", 1)
                    .eq("cid", cid)
            );
            for (CAccount cAccount : randomTempList) {
                redisUtil.lPush(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid, cAccount);
            }
            int randomIndex = RandomUtil.randomInt(randomTempList.size());
            c = randomTempList.get(randomIndex);
        } else {
            String text = ele.toString();
            try {
                c = JSONObject.parseObject(text, CAccount.class);
            } catch (Exception e) {
                log.error("CAccount queue解析异常, text: {}", text);
                return;
            }
        }
        log.info("handleAsyncCreateOrder.start");

        log.info("【任务执行】资源池取出..po channel id {} .randomACInfo - {}", channelId, c);

        // proxy
        payService.addProxy(area, payIp, pr);

        PayInfo payInfo = new PayInfo();
        CGatewayInfo cgi = this.cGatewayMapper.getGateWayInfoByCIdAndGId(c.getCid(), c.getGid());
        payInfo.setChannel(cgi.getCChannel());
        account = c.getAcAccount();
        payInfo.setRepeat_passport(account);
        payInfo.setGame(cgi.getCGame());
        payInfo.setGateway(cgi.getCGateway());
        payInfo.setRecharge_unit(reqMoney);
        payInfo.setRecharge_type(6);
        String acPwd = c.getAcPwd();
        String cookie = "";
        cookie = payService.getCKforQuery(account, Base64.decodeStr(acPwd));
        boolean expire = gee4Service.tokenCheck(cookie, account);
        if (!expire) {
            redisUtil.del("account:ck:" + account);
            cookie = payService.getCKforQuery(account, Base64.decodeStr(acPwd));
            expire = gee4Service.tokenCheck(cookie, account);
            if (!expire) {
                throw new NotFoundException("ck问题，请联系管理员");
            }
        }

        payInfo.setCk(cookie);
        redisUtil.pub("【任务执行】【商户：" + pa + "】【订单ID：" + orderId + "】正在异步创建订单.... ck 校验成功  ");
        log.info("【任务执行】【商户：" + pa + "】【订单ID：" + orderId + "】正在异步创建订单.... ck 校验成功  ");

        JSONObject orderResp = gee4Service.createOrder(payInfo);

        // --- 入库
        PayOrderEvent event = new PayOrderEvent();

        if (orderResp != null && orderResp.get("data") != null) {
            if (orderResp.getInteger("code") != 1) {
                throw new ServiceException(orderResp.toString());
            } else {
                JSONObject data = orderResp.getJSONObject("data");
                String platform_oid = data.getString("vouch_code");
                String resource_url = data.getString("resource_url");

                if ("weixin_mobile".equalsIgnoreCase(data.getString("channel"))) {
                    URL url = URLUtil.url(resource_url);
                    Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), null);
                    String redirect_url = URLDecoder.decode(stringMap.get("redirect_url"), "utf-8");
                    stringMap.put("redirect_url", redirect_url);
                    Map<String, Object> objectObjectSortedMap = new HashMap<>(stringMap);
                    String body = null;
                    try {
                        HttpResponse execute = HttpRequest.post("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb")
                                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                                .form(objectObjectSortedMap)
                                .contentType("application/x-www-form-urlencoded")
                                .header("X-Requested-With", "com.seasun.gamemgr")
                                .header("Origin", "https://m.xoyo.com")
                                .header("Referer", "https://m.xoyo.com")
                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                                .timeout(5000)
                                .execute();
                        body = execute.body();
                    } catch (HttpException e) {
                        e.printStackTrace();
                        throw new ServiceException("微信端异常，请重新下单");
                    }
                    log.info("wx success");
                    if (!StringUtils.hasLength(body)) throw new ServiceException("微信端异常，请重新下单");
                    event.setExt(body);
                }

                String payUrl = handelPayUrl(data, resource_url);
                LocalDateTime asyncTime = LocalDateTime.now();
                pOrderMapper.updateInfoForQueue(orderId, c.getAcid(), OrderStatusEnum.NO_PAY.getCode(), platform_oid, payUrl, payIp, asyncTime);
                pOrderEventMapper.updateInfoForQueue(orderId, data.toJSONString(), platform_oid, event.getExt());

                PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
                boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, poDB);
                if (b) {
                    boolean has = redisUtil.hasKey(CommonConstant.ORDER_WAIT_QUEUE + orderId);
                    if (has) redisUtil.del(CommonConstant.ORDER_WAIT_QUEUE + orderId);
                    log.info("【任务执行】成功订单入查单回调池子, orderId: {}", orderId);
                }

//                pOrderMapper.sumPorderByCAID(c.getAcid());

            }
        } else {
            log.error("create order error, resp -> {}", orderResp);
            throw new ServiceException("订单创建失败，请联系管理员");
        }

    }

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
            if (temp != null && totalCost >= temp.getTotalLimit()) {
                //
//                CAccount cAccount = new CAccount();
//                cAccount.setId(temp.getId());
//                cAccount.setSysStatus(0);
//                cAccount.setSysLog("总余额不足，请联系管理员充值");
//                cAccountMapper.updateById(cAccount);
                it.remove();
            }
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
                String cookie = "";
                boolean ckCheck = gee4Service.tokenCheck(c.getCk(), acAccount);
                if (ckCheck) {
                    cookie = c.getCk();
                    log.info("资源池计算. 库中ck取出...{}", c.getCk());
                } else {
                    cookie = payService.getCKforQuery(acAccount, acPwd);
                    log.info("资源池计算. 接口ck取出...{}", c.getCk());
                }

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

    private String handelPayUrl(JSONObject data, String resource_url) {
        String payUrl = "";
        if ("wyzxpoto".equalsIgnoreCase(data.getString("channel"))) {
            URL url = URLUtil.url(resource_url);
            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
            Map<String, Object> objectObjectSortedMap = new HashMap<>(stringMap);
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

        if ("alipay_mobile".equalsIgnoreCase(data.getString("channel"))) {
            log.info("alipay url 初始: {}", resource_url);
            HttpResponse execute = HttpRequest.get(resource_url)
                    .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                                .form(objectObjectSortedMap)
                    .contentType("application/x-www-form-urlencoded")
                    .header("X-Requested-With", "com.seasun.gamemgr")
                    .header("Origin", "https://m.xoyo.com")
                    .header("Referer", "https://m.xoyo.com")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .timeout(5000)
                    .execute();
            String aliGateway = execute.header("Location");
            log.info("alipay url 一次修正: {}", aliGateway);
            HttpResponse cashierExecute = HttpRequest.get(aliGateway)
                    .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                                .form(objectObjectSortedMap)
                    .contentType("application/x-www-form-urlencoded")
                    .header("X-Requested-With", "com.seasun.gamemgr")
                    .header("Origin", "https://m.xoyo.com")
                    .header("Referer", "https://m.xoyo.com")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .timeout(5000)
                    .execute();
            String cashier = cashierExecute.header("Location");
            log.info("alipay 修正 后 pay url: {}", cashier);
            payUrl = cashier;
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
        }

        return payUrl;
    }
}
