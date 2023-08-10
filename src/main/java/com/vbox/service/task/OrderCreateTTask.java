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
import com.vbox.common.ExpireQueue;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.JXHTEnum;
import com.vbox.common.enums.OrderStatusEnum;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.*;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.TxPayService;
import com.vbox.service.channel.impl.Gee4Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//@Component
@Slf4j
public class OrderCreateTTask {

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
    private TxPayService txPayService;
    @Autowired
    private ChannelShopMapper channelShopMapper;
    @Autowired
    private ChannelPreMapper channelPreMapper;
    @Autowired
    private ChannelMapper channelMapper;

    public static ExpireQueue<SecCode> queue = new ExpireQueue<>();

//    @Scheduled(cron = "0/2 * * * * ? ")  //每 2s
//    @Async("scheduleExecutor")
//    public void testHandleCapPool() throws Exception {
//        SecCode secEle = queue.poll();
//        if (secEle == null){
//            log.info("没元素");
//            return;
//        }
//
//        log.info("当前size: {}, 取到元素:  sec => {}", queue.size(), secEle);
//    }


    //    @Scheduled(cron = "0/2 * * * * ? ")  //每 2s
    @Async("scheduleExecutor")
    public void handleCapPool() throws Exception {
        try {
            SecCode secCode = gee4Service.verifyGeeCapForQuery();
            if (secCode != null) redisUtil.addSecCode(secCode);
//            log.warn("sec code is null, 验证添加异常");
        } catch (NullPointerException e) {
            log.error("handleCapPool.NPE, msg :{}", e.getMessage());
        }
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
            log.error("【任务执行】handleAsyncCreateOrder, err: ", e);
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
            pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
            log.error("【任务执行】异常单，丢弃, {}", po);
        } finally {
            ProxyInfoThreadHolder.remove();
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
        String userAgent = po.getUserAgent();
        String channelId = po.getChannelId();
        Integer cid = po.getChannel();

        String account;
        CAccount c = null;

        CChannel channel = channelMapper.getChannelById(cid);
        if ("jx3".equals(channel.getCGame())) {
            if (channelId.equals("jx3_alipay_pre")) {
//                payService.addProxy(area, payIp, pr);
                createOrderJx3Pre(po, orderId, payIp, reqMoney, userAgent, channelId, cid);
            } else {
                // proxy
                payService.addProxy(area, payIp, pr);
                createOrderJx3(pa, orderId, payIp, reqMoney, userAgent, channelId, cid);
            }
            return;
        } else if ("tx".equals(channel.getCGame())) {// tx
            createOrderTx(orderId, payIp, reqMoney, channelId, cid, channel);
            return;
        } else if ("sdo".equals(channel.getCGame())) { // sdo
            payService.addProxy(area, payIp, pr);
            createOrderSdo(orderId, payIp, reqMoney, userAgent, channelId, cid);
            return;
        } else if ("cy".equals(channel.getCGame())) {
            payService.addProxy(area, payIp, pr);
            createOrderCy(orderId, payIp, reqMoney, userAgent, channelId, cid);
            return;
        }

        log.info("handleAsyncCreateOrder.start");

    }

    private void createOrderJx3Pre(POrderQueue po, String orderId, String payIp, Integer reqMoney, String userAgent, String channelId, Integer cid) {
        ChannelPre preDB;
        boolean flag = false;

        Object ele = redisUtil.rPop(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
        if (ele == null) {
            List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                    .eq("status", 1)
                    .eq("sys_status", 1)
                    .eq("cid", cid)
            );

            if (randomTempList == null || randomTempList.size() == 0) {
                log.error("库存账号不足");
                throw new ServiceException("库存账号不足，请联系管理员");
            }

            List<String> acidList = new ArrayList<>();
            for (CAccount ca : randomTempList) {
                acidList.add(ca.getAcid());
            }

            QueryWrapper<ChannelPre> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("acid", acidList);
            queryWrapper.eq("status", 2);
            queryWrapper.eq("cid", cid);
            queryWrapper.eq("money", reqMoney);

            List<ChannelPre> channelPres = channelPreMapper.selectList(queryWrapper);
            removeSdoElements(channelPres);
            if (channelPres.size() == 0) {
                log.error("库存金额不足");
                throw new ServiceException("库存金额不足，请联系管理员");
            }

            for (ChannelPre pre : channelPres) {
                redisUtil.lPush(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney, pre);
            }

            int randomIndex = RandomUtil.randomInt(channelPres.size());
            preDB = channelPres.get(randomIndex);

        } else {
            String text = ele.toString();
            flag = true;
            try {
                preDB = JSONObject.parseObject(text, ChannelPre.class);
            } catch (Exception e) {
                log.error("ChannelPre queue解析异常, text: {}", text);
                return;
            }
        }

        log.info("【任务执行】资源池取出..缓存池[{}],po channel id {} .random preDB Info - {}", flag, channelId, preDB);
        String payUrl = null;
        try {
            try {
                payUrl = handelJx3PayUrl(preDB.getAddress(), userAgent);
            } catch (Exception e) {
                log.warn("handelJx3PayUrl 重试1次");
                payUrl = handelJx3PayUrl2(preDB.getAddress(), userAgent);
            }
        } catch (Exception e) {
            log.error("预产链接异常: pay url -> {}", payUrl);
//                    int row = channelPreMapper.deleteById(preDB.getId());
            int row = channelPreMapper.updateByPlatId(preDB.getPlatOid(), 1);
            log.error("预产链接异常删除处理: row -> {}, pre info: {}", row, preDB);
            pOrderMapper.updateOStatusByOidForQueue(po.getOrderId(), OrderStatusEnum.PAY_CREATING_ERROR.getCode());
            log.error("【预产任务执行】异常单，丢弃, {}", po);
            return;
        }
        LocalDateTime asyncTime = LocalDateTime.now();
        channelPreMapper.updateByPlatId(preDB.getPlatOid(), 1); // 1 - 已取码
        pOrderMapper.updateInfoForQueue(orderId, preDB.getAcid(), OrderStatusEnum.NO_PAY.getCode(), preDB.getPlatOid(), payUrl, payIp, asyncTime);
        pOrderEventMapper.updateInfoForQueue(orderId, "", preDB.getPlatOid(), preDB.getAddress());

        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
        boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, poDB);
        if (b) {
            boolean has = redisUtil.hasKey(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            if (has) redisUtil.del(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            log.info("【任务执行】成功订单入查单回调池子, orderId: {}", orderId);
        }
    }


    private void createOrderJx3(String pa, String orderId, String payIp, Integer reqMoney, String userAgent, String channelId, Integer cid) throws Exception {
        String account;
        CAccount c;
        boolean flag = false;

        Object ele = redisUtil.rPop(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
        if (ele == null) {
            List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                    .eq("status", 1)
                    .eq("sys_status", 1)
                    .eq("cid", cid)
            );
            for (CAccount cAccount : randomTempList) {
                redisUtil.lPush(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney, cAccount);
            }
            try {
                int randomIndex = RandomUtil.randomInt(randomTempList.size());
                c = randomTempList.get(randomIndex);
            } catch (Exception e) {
                log.error("库存金额不足，或库存账号不足");
                throw new ServiceException("库存金额不足，或库存账号不足，请联系管理员");
            }
        } else {
            String text = ele.toString();
            flag = true;
            try {
                c = JSONObject.parseObject(text, CAccount.class);
            } catch (Exception e) {
                log.error("CAccount queue解析异常, text: {}", text);
                return;
            }
        }

        log.info("【任务执行】资源池取出..缓存池[{}],po channel id {} .random ac Info - {}", flag, channelId, c);

        PayInfo payInfo = new PayInfo();
        CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(c.getCid(), c.getGid());
        payInfo.setChannel(cgi.getCChannel());
        account = c.getAcAccount();
        payInfo.setRepeat_passport(account);
        payInfo.setGame(cgi.getCGame());
        payInfo.setGateway(cgi.getCGateway());
        payInfo.setRecharge_unit(reqMoney);
        payInfo.setRecharge_type(6);
        String acPwd = c.getAcPwd();
        String cookie = "";
//        if ("jx3_weixin".equals(channelId)) {
//            redisUtil.del("account:ck:" + account);
//            log.info("ck new : - del account {}", account);
//        }
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

        JSONObject orderResp;
        if ("jx3_ali_gift".equals(cgi.getCChannelId()) || "jx3_wx_gift".equals(cgi.getCChannelId())) {
            orderResp = gee4Service.createOrderT(cgi.getCGateway(), JXHTEnum.type(reqMoney), payInfo.getCk(), cgi.getCChannel());
        } else {
            orderResp = gee4Service.createOrder(payInfo);
//            orderResp = gee4Service.createOrderForQuery(payInfo);
            for (int i = 0; i < 10; i++) {
                if (orderResp != null) {
                    String os = orderResp.toString();
                    if (os.contains("验证码")) {
                        log.warn("验证码不正确，重试 {} 次 : ", i + 1);
                        orderResp = gee4Service.createOrder(payInfo);
                    } else {
                        break;
                    }
                }
            }
        }

        // --- 入库
        PayOrderEvent event = new PayOrderEvent();

        if (orderResp != null && orderResp.get("data") != null) {
            if (orderResp.getInteger("code") != 1) {
                String os = orderResp.toString();
                if (os.contains("冻结")) {
                    log.warn("冻结关号: channel_account : {}", c);
                    cAccountMapper.stopByCaId("账号冻结，请及时查看", c.getId());
                }
                throw new ServiceException(os);
            } else {
                JSONObject data = orderResp.getJSONObject("data");
                String platform_oid = data.getString("vouch_code");
                String resource_url = data.getString("resource_url");

                String payUrl = handelPayUrl(data, resource_url, userAgent);
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

            }
        } else {
            log.error("create order error, resp -> {}", orderResp);
            throw new ServiceException("订单创建失败，请联系管理员");
        }
    }

    private void createOrderSdo(String orderId, String payIp, Integer reqMoney, String userAgent, String channelId, Integer cid) {
        if (reqMoney == 200) {
            reqMoney = 204;
        } else if (reqMoney == 10) {
            reqMoney = 10;
        } else if (reqMoney == 30) {
            reqMoney = 30;
        } else if (reqMoney == 1) {
            reqMoney = 1;
        } else if (reqMoney == 100) {
            reqMoney = 102;
        } else {
            throw new ServiceException("仅支持10、30、100、200的固额设置");
        }
        ChannelPre preDB;
        boolean flag = false;
        Object ele = redisUtil.rPop(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
        if (ele == null) {
            List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                    .eq("status", 1)
                    .eq("sys_status", 1)
                    .eq("cid", cid)
            );

            if (randomTempList == null || randomTempList.size() == 0) {
                log.error("库存账号不足");
                throw new ServiceException("库存账号不足，请联系管理员");
            }

            List<String> acidList = new ArrayList<>();
            for (CAccount ca : randomTempList) {
                acidList.add(ca.getAcid());
            }

            QueryWrapper<ChannelPre> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("acid", acidList);
            queryWrapper.eq("status", 2);
            queryWrapper.eq("cid", cid);
            queryWrapper.eq("money", reqMoney);

            List<ChannelPre> channelPres = channelPreMapper.selectList(queryWrapper);
            removeSdoElements(channelPres);
            if (channelPres.size() == 0) {
                log.error("库存金额不足");
                throw new ServiceException("库存金额不足，请联系管理员");
            }

            for (ChannelPre pre : channelPres) {
                redisUtil.lPush(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney, pre);
            }

            int randomIndex = RandomUtil.randomInt(channelPres.size());
            preDB = channelPres.get(randomIndex);


        } else {
            String text = ele.toString();
            flag = true;
            try {
                preDB = JSONObject.parseObject(text, ChannelPre.class);
            } catch (Exception e) {
                log.error("ChannelPre queue解析异常, text: {}", text);
                return;
            }
        }

        //拿平台id做key
        String orderKey = preDB.getPlatOid();
        if (redisUtil.hasKey(orderKey)) {
            throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
        }
        redisUtil.set(orderKey, 1, 300L);

        log.info("【任务执行】资源池取出..缓存池[{}],po channel id {} .random preDB Info - {}", flag, channelId, preDB);

        String payUrl = null;
        try {
            try {
                payUrl = handelSdoPayUrl(preDB.getAddress(), userAgent);
            } catch (Exception e) {
                log.warn("handelSdoPayUrl 重试1次");
                payUrl = handelSdoPayUrl2(preDB.getAddress(), userAgent);
            }
        } catch (Exception e) {
            log.error("预产链接异常: pay url -> {}", payUrl);
//            int row = channelPreMapper.deleteById(preDB.getId());
            int row = channelPreMapper.updateByPlatId(preDB.getPlatOid(), 1);
            log.error("预产链接异常删除处理: row -> {}, pre info: {}", row, preDB);
            pOrderMapper.updateOStatusByOidForQueue(orderId, OrderStatusEnum.PAY_CREATING_ERROR.getCode());
            log.error("【预产任务执行】异常单，丢弃, {}", orderId);
            return;
        }
        LocalDateTime asyncTime = LocalDateTime.now();
        channelPreMapper.updateByPlatId(preDB.getPlatOid(), 1); // 1 - 已取码
        pOrderMapper.updateInfoForQueue(orderId, preDB.getAcid(), OrderStatusEnum.NO_PAY.getCode(), preDB.getPlatOid(), payUrl, payIp, asyncTime);
        pOrderEventMapper.updateInfoForQueue(orderId, "", preDB.getPlatOid(), preDB.getAddress());

        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
        boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, poDB);
        if (b) {
            boolean has = redisUtil.hasKey(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            if (has) redisUtil.del(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            log.info("【任务执行】成功订单入查单回调池子, orderId: {}", orderId);
        }
    }

    private void createOrderCy(String orderId, String payIp, Integer reqMoney, String userAgent, String channelId, Integer cid) {

        ChannelPre preDB;

        Object ele = redisUtil.rPop(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
        if (ele == null) {
            List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                    .eq("status", 1)
                    .eq("sys_status", 1)
                    .eq("cid", cid)
            );

            if (randomTempList == null || randomTempList.size() == 0) {
                log.error("库存账号不足");
                throw new ServiceException("库存账号不足，请联系管理员");
            }

            List<String> acidList = new ArrayList<>();
            for (CAccount ca : randomTempList) {
                acidList.add(ca.getAcid());
            }

            QueryWrapper<ChannelPre> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("acid", acidList);
            queryWrapper.eq("status", 2);
            queryWrapper.eq("cid", cid);
            queryWrapper.eq("money", reqMoney);

            List<ChannelPre> channelPres = channelPreMapper.selectList(queryWrapper);
            removeCyElements(channelPres);
            if (channelPres.size() == 0) {
                log.error("库存金额不足");
                throw new ServiceException("库存金额不足，请联系管理员");
            }

            for (ChannelPre pre : channelPres) {
                redisUtil.lPush(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney, pre);
            }

            int randomIndex = RandomUtil.randomInt(channelPres.size());
            preDB = channelPres.get(randomIndex);

        } else {
            String text = ele.toString();
            try {
                preDB = JSONObject.parseObject(text, ChannelPre.class);
            } catch (Exception e) {
                log.error("ChannelPre queue解析异常, text: {}", text);
                return;
            }
        }

        log.info("【任务执行】资源池取出..po channel id {} .random preDB Info - {}", channelId, preDB);
        String payUrl = null;
        try {
            try {
                payUrl = handelCyPayUrl(preDB.getAddress(), userAgent);
            } catch (Exception e) {
                log.warn("handelCyPayUrl 重试1次");
                payUrl = handelCyPayUrl(preDB.getAddress(), userAgent);
            }
        } catch (Exception e) {
            log.error("预产链接异常: pay url -> {}", payUrl);
//            int row = channelPreMapper.deleteById(preDB.getId());
            int row = channelPreMapper.updateByPlatId(preDB.getPlatOid(), 1);
            log.error("预产链接异常删除处理: row -> {}, pre info: {}", row, preDB);
            pOrderMapper.updateOStatusByOidForQueue(orderId, OrderStatusEnum.PAY_CREATING_ERROR.getCode());
            log.error("【预产任务执行】异常单，丢弃, {}", orderId);
            return;
        }
        LocalDateTime asyncTime = LocalDateTime.now();
        channelPreMapper.updateByPlatId(preDB.getPlatOid(), 1); // 1 - 已取码
        pOrderMapper.updateInfoForQueue(orderId, preDB.getAcid(), OrderStatusEnum.NO_PAY.getCode(), preDB.getPlatOid(), payUrl, payIp, asyncTime);
        pOrderEventMapper.updateInfoForQueue(orderId, "", preDB.getPlatOid(), preDB.getAddress());

        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
        boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, poDB);
        if (b) {
            boolean has = redisUtil.hasKey(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            if (has) redisUtil.del(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            log.info("【任务执行】成功订单入查单回调池子, orderId: {}", orderId);
        }
    }

    private void createOrderTx(String orderId, String payIp, Integer reqMoney, String channelId, Integer cid, CChannel channel) {
        CAccount c;
        boolean flag = false;
        Object ele = redisUtil.rPop(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney);
        if (ele == null) {
            List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                    .eq("status", 1)
                    .eq("sys_status", 1)
                    .eq("cid", cid)
            );

            List<TxWaterList> rl = new ArrayList<>();
            // 使用HashMap来保存相同充值金额的充值账号
            Map<Integer, List<String>> map = new HashMap<>();

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

            //获取已经有充值当前金额的账户，作去除处理
            List<String> qqList = map.get(reqMoney);
            log.warn("当前30分钟内 - 已支付的记录，金额: {} , 记录：{}", reqMoney, qqList);

            removeTxElements(qqList, randomTempList, reqMoney);

            try {
                int randomIndex = RandomUtil.randomInt(randomTempList.size());
                c = randomTempList.get(randomIndex);

                randomTempList.remove(randomIndex);

                for (CAccount cAccount : randomTempList) {
                    redisUtil.lPush(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":" + reqMoney, cAccount);
                }
            } catch (Exception e) {
                log.error("库存金额不足，或库存账号不足");
                throw new ServiceException("库存金额不足，或库存账号不足，请联系管理员");
            }
        } else {
            String text = ele.toString();
            flag = true;
            try {
                c = JSONObject.parseObject(text, CAccount.class);
            } catch (Exception e) {
                log.error("CAccount queue解析异常, text: {}", text);
                return;
            }
        }
        log.info("【任务执行】资源池取出..缓存池[{}],po channel id {} .random ac Info - {}", flag, channelId, c);

        String payUrl = handelTxPayUrl(channel.getCChannelId(), reqMoney);
        LocalDateTime asyncTime = LocalDateTime.now();
        pOrderMapper.updateInfoForQueue(orderId, c.getAcid(), OrderStatusEnum.NO_PAY.getCode(), asyncTime.toEpochSecond(ZoneOffset.UTC) + "|QQ|" + c.getAcAccount(), payUrl, payIp, asyncTime);
        pOrderEventMapper.updateInfoForQueue(orderId, "", asyncTime.toEpochSecond(ZoneOffset.UTC) + "|QQ|" + c.getAcAccount(), "");

        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
        boolean b = redisUtil.lPush(CommonConstant.ORDER_QUERY_QUEUE, poDB);
        if (b) {
            boolean has = redisUtil.hasKey(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            if (has) redisUtil.del(CommonConstant.ORDER_WAIT_QUEUE + orderId);
            log.info("【任务执行】成功订单入查单回调池子, orderId: {}", orderId);
        }
    }

    private String handelCyPayUrl(String address, String userAgent) {
        String payUrl = "";
        boolean isMob = CommonUtil.isMobileDevice(userAgent);
        if (!isMob) {
            userAgent = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36";
        }
        log.info("cy url 初始: {}", address);
        HttpResponse execute = HttpRequest.post(address)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("Referer", "http://chong.changyou.com/")
                .header("User-Agent", userAgent)
                .execute();

        String locUrl = execute.header("Location");
        log.info("cy url 一次修正: {}", locUrl);

        HttpResponse executeLoc = HttpRequest.get(locUrl)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("Referer", "http://chong.changyou.com/")
                .header("User-Agent", userAgent)
                .execute();

        String cashierUrl = executeLoc.header("Location");
        log.info("cy url 二次修正: {}", cashierUrl);

        HttpResponse cashierResp = HttpRequest.get(cashierUrl)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("Referer", "http://chong.changyou.com/")
                .header("User-Agent", userAgent)
                .execute();

        String htmlBody = cashierResp.body();
        String qrCodeValue = CommonUtil.getQrCodeValue(htmlBody);

        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";

        if (qrCodeValue == null) {
            log.error("qrCodeValue ex， address： {}", address);
            throw new ServiceException("当前通道无可用的pay地址");
        }
        payUrl = starApp + qrCodeValue;
        log.info("cy url 最终值: {}", payUrl);
        return payUrl;
    }


    private String handelJx3PayUrl(String address, String userAgent) {
        String payUrl = "";
//        boolean isMob = CommonUtil.isMobileDevice(userAgent);
//        if (!isMob) {
//            userAgent = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36";
//        }
//
//        log.info("alipay url 初始: {}", address);
//        HttpResponse execute = HttpRequest.get(address)
//                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                .contentType("application/x-www-form-urlencoded")
//                .header("X-Requested-With", "com.seasun.gamemgr")
//                .header("User-Agent", userAgent)
//                .header("Origin", "https://m.xoyo.com")
//                .header("Referer", "https://m.xoyo.com")
//                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                .timeout(5000)
//                .execute();
//        String aliGateway = execute.header("Location");
//        log.info("alipay url 一次修正: {}", aliGateway);
//        HttpResponse cashierExecute = HttpRequest.get(aliGateway)
//                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                .contentType("application/x-www-form-urlencoded")
//                .header("X-Requested-With", "com.seasun.gamemgr")
//                .header("User-Agent", userAgent)
//                .header("Origin", "https://m.xoyo.com")
//                .header("Referer", "https://m.xoyo.com")
//                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                .timeout(5000)
//                .execute();
//        String cashier = cashierExecute.header("Location");
//        log.info("alipay 修正后 pay url: {}", cashier);
//
//        HttpResponse cashierResp = HttpRequest.get(cashier)
//                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                .header("User-Agent", userAgent)
//                .execute();
//
//        String htmlBody = cashierResp.body();
//        String qrCodeValue = CommonUtil.getQrCodeValue(htmlBody);
//
//        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";
//
//        if (qrCodeValue == null) {
//            log.error("qrCodeValue ex， address： {}", address);
//            throw new ServiceException("当前通道无可用的pay地址");
//        }
//        payUrl = starApp + qrCodeValue;
        String qrCodeValue;
        try {
            qrCodeValue = URLEncoder.encode(address, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException("pay 地址解析异常");
        }
        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";
        payUrl = starApp + qrCodeValue;
        log.info("alipay url 最终值: {}", payUrl);
        return payUrl;
    }

    private String handelJx3PayUrl2(String address, String userAgent) {
        String payUrl = "";
//        boolean isMob = CommonUtil.isMobileDevice(userAgent);
//        if (!isMob) {
//            userAgent = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36";
//        }
//
//        log.info("alipay url 初始: {}", address);
//        HttpResponse execute = HttpRequest.get(address)
//                .contentType("application/x-www-form-urlencoded")
//                .header("X-Requested-With", "com.seasun.gamemgr")
//                .header("User-Agent", userAgent)
//                .header("Origin", "https://m.xoyo.com")
//                .header("Referer", "https://m.xoyo.com")
//                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                .timeout(5000)
//                .execute();
//        String aliGateway = execute.header("Location");
//        log.info("alipay url 一次修正: {}", aliGateway);
//        HttpResponse cashierExecute = HttpRequest.get(aliGateway)
//                .contentType("application/x-www-form-urlencoded")
//                .header("X-Requested-With", "com.seasun.gamemgr")
//                .header("User-Agent", userAgent)
//                .header("Origin", "https://m.xoyo.com")
//                .header("Referer", "https://m.xoyo.com")
//                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                .timeout(5000)
//                .execute();
//        String cashier = cashierExecute.header("Location");
//        log.info("alipay 修正后 pay url: {}", cashier);
//
//        HttpResponse cashierResp = HttpRequest.get(cashier)
//                .header("User-Agent", userAgent)
//                .execute();
//
//        String htmlBody = cashierResp.body();
//        String qrCodeValue = CommonUtil.getQrCodeValue(htmlBody);
//
//        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";
//
//        if (qrCodeValue == null) {
//            log.error("qrCodeValue ex， address： {}", address);
//            throw new ServiceException("当前通道无可用的pay地址");
//        }
//        payUrl = starApp + qrCodeValue;
        String qrCodeValue;
        try {
            qrCodeValue = URLEncoder.encode(address, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ServiceException("pay 地址解析异常");
        }
        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";
        payUrl = starApp + qrCodeValue;
        log.info("alipay url 最终值: {}", payUrl);
        return payUrl;
    }

    private String handelSdoPayUrl(String address, String userAgent) {
        String payUrl = "";
        boolean isMob = CommonUtil.isMobileDevice(userAgent);
        if (!isMob) {
            userAgent = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36";
        }
        log.info("sdo url 初始: {}", address);
        HttpResponse execute = HttpRequest.get(address)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("User-Agent", userAgent)
                .execute();

        String locUrl = execute.header("Location");
        log.info("sdo url 一次修正: {}", locUrl);

        HttpResponse executeLoc = HttpRequest.get(locUrl)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("User-Agent", userAgent)
                .execute();

        String cashierUrl = executeLoc.header("Location");
        log.info("sdo url 二次修正: {}", cashierUrl);

        HttpResponse cashierResp = HttpRequest.get(cashierUrl)
                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                .header("User-Agent", userAgent)
                .execute();

        String htmlBody = cashierResp.body();
        String qrCodeValue = CommonUtil.getQrCodeValue(htmlBody);

        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";

        if (qrCodeValue == null) {
            log.error("qrCodeValue ex， address： {}", address);
            throw new ServiceException("当前通道无可用的pay地址");
        }
        payUrl = starApp + qrCodeValue;
//        String qrCodeValue;
//        try {
//            qrCodeValue = URLEncoder.encode(address, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            throw new ServiceException("pay 地址解析异常");
//        }
//        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";
//        payUrl = starApp + qrCodeValue;
        log.info("sdo url 最终值: {}", payUrl);
        return payUrl;
    }

    private String handelSdoPayUrl2(String address, String userAgent) {
        String payUrl = "";
        boolean isMob = CommonUtil.isMobileDevice(userAgent);
        if (!isMob) {
            userAgent = "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36";
        }
        log.info("sdo url 初始: {}", address);
        HttpResponse execute = HttpRequest.get(address)
                .header("User-Agent", userAgent)
                .execute();

        String locUrl = execute.header("Location");
        log.info("sdo url 一次修正: {}", locUrl);

        HttpResponse executeLoc = HttpRequest.get(locUrl)
                .header("User-Agent", userAgent)
                .execute();

        String cashierUrl = executeLoc.header("Location");
        log.info("sdo url 二次修正: {}", cashierUrl);

        HttpResponse cashierResp = HttpRequest.get(cashierUrl)
                .header("User-Agent", userAgent)
                .execute();

        String htmlBody = cashierResp.body();
        String qrCodeValue = CommonUtil.getQrCodeValue(htmlBody);

        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";

        if (qrCodeValue == null) {
            log.error("qrCodeValue ex， address： {}", address);
            throw new ServiceException("当前通道无可用的pay地址");
        }
        payUrl = starApp + qrCodeValue;
//        String qrCodeValue;
//        try {
//            qrCodeValue = URLEncoder.encode(address, "UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            throw new ServiceException("pay 地址解析异常");
//        }
//        String starApp = "alipays://platformapi/startapp?appId=20000067&url=";
//        payUrl = starApp + qrCodeValue;
        log.info("sdo url 最终值: {}", payUrl);
        return payUrl;
    }

    private String handelTxPayUrl(String cChannelId, Integer money) {
        String payUrl = "";
        if (cChannelId.contains("tx")) {
            if (cChannelId.equals("tx_jym")) {
                payUrl = "alipays://platformapi/startapp?appId=2021003103651463&page=%2Fpages%2Findex%2Findex%3FredirectUrl%3Dhttps%253A%252F%252Fm.jiaoyimao.com%252Frecharge%252Ffr%252Fcenter%253Fspm%253Dgcmall.home2022.kingkongarea.0%26jump%3DY&enbsv=0.2.2305051433.4&chInfo=ch_share__chsub_CopyLink&apshareid=1597A291-EF68-40B3-ABAF-72BD421AE78C&shareBizType=H5App_XCX&fxzjshareChinfo=ch_share__chsub_CopyLink";
//                payUrl = "https://ur.alipay.com/_3Xb3IPhvZPoJ2giE4DVNXv";
                log.info(" tx_jym - pay url: {}", payUrl);
                return payUrl;
            } else {
                QueryWrapper<ChannelShop> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("channel", cChannelId);
                queryWrapper.eq("money", money);
                queryWrapper.eq("status", 1);
                List<ChannelShop> randomTempList = channelShopMapper.selectList(queryWrapper);
                if (randomTempList != null && randomTempList.size() != 0) {
                    int randomIndex = RandomUtil.randomInt(randomTempList.size());
                    ChannelShop channelShop = randomTempList.get(randomIndex);
                    payUrl = channelShop.getAddress();
                }
                log.warn("初始 tx pay url : {}", payUrl);

                try {
                    switch (cChannelId) {
                        case "tx_dy": {
                            HttpResponse executeDY = HttpRequest.get(payUrl)
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                                    .execute();
                            String dyUrl = executeDY.header("Location");
                            URL url = URLUtil.url(dyUrl);
                            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
                            String detailSchema = stringMap.get("detail_schema");
                            payUrl = detailSchema.replace("sslocal://", "snssdk1128://");
                            log.warn("修正dy pay url: {}", payUrl);
                            break;
                        }
                        case "tx_tb": {
                            HttpResponse executeDY = HttpRequest.get(payUrl)
                                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 8.0.0; SM-G955U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.141 Mobile Safari/537.36")
                                    .execute();
                            String tbHtml = executeDY.body();
                            String tbTmpUrl = "";
                            Pattern pattern = Pattern.compile("var url = '([^']*)'");
                            Matcher matcher = pattern.matcher(tbHtml);
                            while (matcher.find()) {
                                String match = matcher.group(0);
                                if (match.contains("taobao.com")) {
                                    String value = matcher.group(1);
                                    log.warn("匹配到的行：" + match);
                                    tbTmpUrl = value;
                                    log.warn("提取的值：" + tbTmpUrl);
                                }
                            }

                            URL url = URLUtil.url(tbTmpUrl);
                            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
                            String itemId = stringMap.get("id");

                            if (itemId == null || itemId.isEmpty()) {
                                pattern = Pattern.compile("m.taobao.com/i(\\d+)\\.htm");
                                matcher = pattern.matcher(tbTmpUrl);
                                if (matcher.find()) {
                                    itemId = matcher.group(1);
                                    log.warn("修复值 item id ：" + itemId);
                                } else {
                                    System.out.println("未找到skuId");
                                }
                            }

                            String schema = "https://main.m.taobao.com/order/index.html?buildOrderVersion=3.0&skuId=undefined&exParams=%7B%22id%22%3A%22674305212211%22%7D&quantity=1&itemId=" + itemId;
                            payUrl = "tbopen://m.taobao.com/tbopen/index.html?h5Url=" + URLEncoder.encode(schema, "UTF-8");
                            log.warn("修正tb pay url: {}", payUrl);
                            break;
                        }
                        case "tx_jd": {
                            String skuId = "";
                            Pattern pattern = Pattern.compile("/product/(\\d+)\\.html");
                            Matcher matcher = pattern.matcher(payUrl);
                            if (matcher.find()) {
                                skuId = matcher.group(1);
                                log.warn("skuId: {}", skuId);
                            } else {
                                System.out.println("未找到skuId");
                            }

                            String schemaBody = "{\"sourceValue\":\"0_productDetail_97\",\"des\":\"productDetail\",\"skuId\":\"" + skuId + "\",\"category\":\"jump\",\"sourceType\":\"PCUBE_CHANNEL\"}";

                            payUrl = "openapp.jdmobile://virtual?params=" + URLEncoder.encode(schemaBody, "UTF-8");
                            log.warn("修正jd pay url: {}", payUrl);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("tx pay url 解析转换异常", e);
                }
            }
        } else {
            log.error("当前通道无可用的引导地址， channel： {}, 引导金额： {}", cChannelId, money);
            throw new ServiceException("当前通道无可用的引导地址");
        }


//        if (cChannelId.equals("tx_jym")) {
//            payUrl = "alipays://platformapi/startapp?appId=2021003103651463&page=%2Fpages%2Findex%2Findex%3FredirectUrl%3Dhttps%253A%252F%252Fm.jiaoyimao.com%252Frecharge%252Ffr%252Fcenter%253Fspm%253Dgcmall.home2022.kingkongarea.0%26jump%3DY&enbsv=0.2.2305051433.4&chInfo=ch_share__chsub_CopyLink&apshareid=1597A291-EF68-40B3-ABAF-72BD421AE78C&shareBizType=H5App_XCX&fxzjshareChinfo=ch_share__chsub_CopyLink";
//            log.info(" tx_jym - pay url: {}", payUrl);
//            return payUrl;
//        }
//        if (cChannelId.equals("tx_tb")) {
//            if (money == 30) {
//                payUrl = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https%3A%2F%2Fmarket.m.taobao.com%2Fapps%2Fmarket%2Fgames%2Fdeal.html%3Fwh_weex%3Dtrue%26referenceType%3D6%26referenceId%3D201161210%26channelId%3D2503001%26itemId%3D657899305846%26skuId%3D0%26quantity%3D1%26slk_gid%3Dgid_er_er%257Cgid_er_af_pop&action=ali.open.nav&module=h5&bootImage=0&slk_sid=ItihHOiEHRACAW/PeyWsKO3o_1689351746752&slk_t=1689351746779&slk_gid=gid_er_er%7Cgid_er_af_pop&afcPromotionOpen=false&source=slk_dp";
//            }
//            if (money == 50) {
//                payUrl = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https%3A%2F%2Fmarket.m.taobao.com%2Fapps%2Fmarket%2Fgames%2Fdeal.html%3Fwh_weex%3Dtrue%26referenceType%3D6%26itemId%3D657900053478";
//            }
//            if (money == 100) {
//                payUrl = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https%3A%2F%2Fmarket.m.taobao.com%2Fapps%2Fmarket%2Fgames%2Fdeal.html%3Fwh_weex%3Dtrue%26referenceType%3D6%26referenceId%3D201161210%26channelId%3D2503001%26itemId%3D657900389866%26skuId%3D0%26quantity%3D1%26slk_gid%3Dgid_er_er%257Cgid_er_af_pop&action=ali.open.nav&module=h5&bootImage=0&slk_sid=ItihHOiEHRACAW/PeyWsKO3o_1689351599055&slk_t=1689351599099&slk_gid=gid_er_er%7Cgid_er_af_pop&afcPromotionOpen=false&source=slk_dp";
//            }
//            if (money == 200) {
//                payUrl = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https%3A%2F%2Fmarket.m.taobao.com%2Fapps%2Fmarket%2Fgames%2Fdeal.html%3Fwh_weex%3Dtrue%26referenceType%3D6%26referenceId%3D201161210%26channelId%3D2503001%26itemId%3D658661863688%26skuId%3D0%26quantity%3D1%26slk_gid%3Dgid_er_er%257Cgid_er_af_pop&action=ali.open.nav&module=h5&bootImage=0&slk_sid=ItihHOiEHRACAW/PeyWsKO3o_1689351543326&slk_t=1689351543388&slk_gid=gid_er_er%7Cgid_er_af_pop&afcPromotionOpen=false&source=slk_dp";
//            }
//            if (money == 300) {
//                payUrl = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https%3A%2F%2Fmarket.m.taobao.com%2Fapps%2Fmarket%2Fgames%2Fdeal.html%3Fwh_weex%3Dtrue%26referenceType%3D6%26referenceId%3D201161210%26channelId%3D2503001%26itemId%3D657901101572%26skuId%3D0%26quantity%3D1%26slk_gid%3Dgid_er_er%257Cgid_er_af_pop&action=ali.open.nav&module=h5&bootImage=0&slk_sid=ItihHOiEHRACAW/PeyWsKO3o_1689351457896&slk_t=1689351457917&slk_gid=gid_er_er%7Cgid_er_af_pop&afcPromotionOpen=false&source=slk_dp";
//            }
//            log.info(" tx_tb - pay url: {}", payUrl);
//            return payUrl;
//        }
//        if (cChannelId.equals("tx_zfb")) {
//            if (money == 50) {
//                payUrl = "alipays://platformapi/startapp?appId=2021003185606970&page=%2Fpages%2FproductDetail%2FproductDetail%3FproductId%3D1467&enbsv=0.2.2306091129.58&chInfo=ch_share__chsub_CopyLink&fxzjshareChinfo=ch_share__chsub_CopyLink&apshareid=598a805e-ee91-4059-8b75-3a38f9d8d995&shareBizType=H5App_XCX&launchKey=102d940e-2384-41a7-aac8-5d2ae1275b62-1689349737646";
//            }
//            if (money == 100) {
//                payUrl = "alipays://platformapi/startapp?appId=2021003185606970&page=%2Fpages%2FproductDetail%2FproductDetail%3FproductId%3D1603&enbsv=0.2.2306091129.58&chInfo=ch_share__chsub_CopyLink&fxzjshareChinfo=ch_share__chsub_CopyLink&apshareid=f41ef249-46e0-44ae-99a7-3ef5c2b25fce&shareBizType=H5App_XCX&launchKey=d68feafa-87d3-408d-b851-2c195e927efe-1689351952630";
//            }
//            log.info(" tx_zfb - pay url: {}", payUrl);
//            return payUrl;
//        }
//        if (cChannelId.equals("tx_dy")) {
//            if (money == 50) {
////                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3618318391900813952&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAA2I9NdgAKZrz9e0tLm1csyDMNqLESPDm34TdYYqXe8-I%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689351152&__reporte_stage=launch";
//                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3618318076203948235&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAA2I9NdgAKZrz9e0tLm1csyDMNqLESPDm34TdYYqXe8-I%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689491760&__reporte_stage=launch";
//            }
//            if (money == 100) {
//                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3618318220076937099&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAA2I9NdgAKZrz9e0tLm1csyDMNqLESPDm34TdYYqXe8-I%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689351278&__reporte_stage=launch";
//            }
//            if (money == 200) {
//                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3618318391900813952&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAA2I9NdgAKZrz9e0tLm1csyDMNqLESPDm34TdYYqXe8-I%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689351152&__reporte_stage=launch";
//            }
//            if (money == 300) {
//                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3624398545265771310&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAAwEH5Emgyy7hH0y9Ui8OLL68XYJNpXu7PEfpQklrigye43_tOIVZbwbSop1ubEjim%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689352259&__reporte_stage=launch";
//            }
//            if (money == 500) {
//                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3615351507823762752&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAAwEH5Emgyy7hH0y9Ui8OLL68XYJNpXu7PEfpQklrigye43_tOIVZbwbSop1ubEjim%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689352197&__reporte_stage=launch";
//            }
//            if (money == 500) {
//                payUrl = "snssdk1128://ec_goods_detail?channel_id=&channel_type=&enter_from=new_h5_product_detail&meta_params=%7B%22entrance_info%22%3A%22%7B%5C%22share_content%5C%22%3A%5C%22product_detail%5C%22%2C%5C%22share_object%5C%22%3A%5C%22copy%5C%22%7D%22%7D&promotion_id=3615351507823762752&request_additions=%7B%22sec_author_id%22%3A%22MS4wLjABAAAAwEH5Emgyy7hH0y9Ui8OLL68XYJNpXu7PEfpQklrigye43_tOIVZbwbSop1ubEjim%22%2C%22enter_from%22%3A%22new_h5_product_detail%22%7D&scene_from=share_reflow&use_link_command=1&zlink=https%3A%2F%2Fz.douyin.com%2FYCvp&zlink_click_time=1689352197&__reporte_stage=launch";
//            }
//            log.info(" tx_dy - pay url: {}", payUrl);
//            return payUrl;
//        }
//        if (cChannelId.equals("tx_jd")) {
//            if (money == 50) {
//                payUrl = "openapp.jdmobile://virtual?params=%7B%22sourceValue%22%3A%220_productDetail_97%22%2C%22des%22%3A%22productDetail%22%2C%22skuId%22%3A%2210063946360171%22%2C%22category%22%3A%22jump%22%2C%22sourceType%22%3A%22PCUBE_CHANNEL%22%7D%20";
//            }
//            if (money == 100) {
//                payUrl = "openapp.jdmobile://virtual?params=%7B%22sourceValue%22%3A%220_productDetail_97%22%2C%22des%22%3A%22productDetail%22%2C%22skuId%22%3A%2210063946477179%22%2C%22category%22%3A%22jump%22%2C%22sourceType%22%3A%22PCUBE_CHANNEL%22%7D%20";
//            }
//            if (money == 200) {
//                payUrl = "openapp.jdmobile://virtual?params=%7B%22sourceValue%22%3A%220_productDetail_97%22%2C%22des%22%3A%22productDetail%22%2C%22skuId%22%3A%2210063946614417%22%2C%22category%22%3A%22jump%22%2C%22sourceType%22%3A%22PCUBE_CHANNEL%22%7D%20";
//            }
//            if (money == 300) {
//                payUrl = "openapp.jdmobile://virtual?params=%7B%22sourceValue%22%3A%220_productDetail_97%22%2C%22des%22%3A%22productDetail%22%2C%22skuId%22%3A%2210063946727378%22%2C%22category%22%3A%22jump%22%2C%22sourceType%22%3A%22PCUBE_CHANNEL%22%7D%20";
//            }
//            if (money == 500) {
//                payUrl = "openapp.jdmobile://virtual?params=%7B%22sourceValue%22%3A%220_productDetail_97%22%2C%22des%22%3A%22productDetail%22%2C%22skuId%22%3A%2210063946825293%22%2C%22category%22%3A%22jump%22%2C%22sourceType%22%3A%22PCUBE_CHANNEL%22%7D%20";
//            }
////            payUrl = "openapp.jdmobile://virtual?params=%7B%22sourceValue%22%3A%220_productDetail_97%22%2C%22des%22%3A%22productDetail%22%2C%22skuId%22%3A%2210063946360171%22%2C%22category%22%3A%22jump%22%2C%22sourceType%22%3A%22PCUBE_CHANNEL%22%7D%20";
//            log.info(" tx_jd - pay url: {}", payUrl);
//            return payUrl;
//        }
        return payUrl;
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

    public void removeCyElements(List<ChannelPre> sdoList) {
        Iterator<ChannelPre> iterator = sdoList.iterator();
        LocalDateTime now = LocalDateTime.now();

        while (iterator.hasNext()) {
            ChannelPre pre = iterator.next();
            LocalDateTime createTime = pre.getCreateTime();
            LocalDateTime pre30min = now.plusDays(-1);
            if (createTime.isBefore(pre30min)) { //当前预产时间已经是半小时前的, remove并置为超时状态
                channelPreMapper.updateByPlatId(pre.getPlatOid(), 3);
                iterator.remove();
            }
        }
    }

    public void removeSdoElements(List<ChannelPre> sdoList) {
        Iterator<ChannelPre> iterator = sdoList.iterator();
        LocalDateTime now = LocalDateTime.now();

        while (iterator.hasNext()) {
            ChannelPre pre = iterator.next();
            LocalDateTime createTime = pre.getCreateTime();
            LocalDateTime pre30min = now.plusDays(-1);
            if (createTime.isBefore(pre30min)) { //当前预产时间已经是半小时前的, remove并置为超时状态
                channelPreMapper.updateByPlatId(pre.getPlatOid(), 3);
                iterator.remove();
            }
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

    private String handelPayUrl(JSONObject data, String resource_url, String userAgent) throws UnsupportedEncodingException {
        String payUrl = "";
        if ("weixin".equalsIgnoreCase(data.getString("channel"))) {
            payUrl = resource_url;
            log.info(" weixin - pay url: {}", payUrl);
            return payUrl;
        }
        if ("wyzxpoto".equalsIgnoreCase(data.getString("channel"))) {
            URL url = URLUtil.url(resource_url);
            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), StandardCharsets.UTF_8);
            Map<String, Object> objectObjectSortedMap = new HashMap<>(stringMap);
            HttpResponse execute = HttpRequest.post("https://wepay.jd.com/jdpay/saveOrder")
//                    .header("User-Agent", userAgent)
                    .setFollowRedirects(false).form(objectObjectSortedMap).execute();
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
//                log.info(" xxx 修正 前 pay url: {}", jdPay);
//                URL urlPay = URLUtil.url(jdPay);
//                Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
//                Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);
//                String qrHtml = HttpRequest.post("https://wepay.jd.com/jdpay/login")
////                        .header("Accept", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
////                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
////                        .header("User-Agent", userAgent)
//                        .contentType("application/x-www-form-urlencoded")
//                        .setFollowRedirects(false)
//                        .form(urlPayMap)
//                        .execute().body();
//                String subBox = qrHtml.substring(qrHtml.indexOf("qrUrl=") + 6);
//                jdPay = subBox.substring(0, subBox.indexOf("\""));

                log.info(" xxx 修正 后 pay url: {}", jdPay);
                payUrl = jdPay;
                return payUrl;
            } else {
                log.info(" yyy 修正 后 pay url: {}", jdPay);

//                HttpResponse executeLocation = HttpRequest.post(jdPay)
//                        .execute();
//                String loc = executeLocation.header("Location");
//                log.info("yyy location 修正: {}, http status: {}", loc, executeLocation.getStatus());
//                String qrHtml = HttpRequest.post(loc)
//                        .execute().body();
//                                String subBox = qrHtml.substring(qrHtml.indexOf("qrUrl=") + 6);
//                jdPay = subBox.substring(0, subBox.indexOf("\""));
//                log.info(" yyy 修正后 pay url: {}", jdPay);
                payUrl = jdPay;
//                return jdPay;
//                URL urlPay = URLUtil.url(jdPay);
//                Map<String, String> urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
//                Map<String, Object> urlPayMap = new HashMap<>(urlPayStringMap);
//                log.info(" yyy map param: {}", urlPayMap);
//                HttpResponse executeLocation = HttpRequest.post("https://wepay.jd.com/jdpay/payIndex")
////                        .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
////                        .header("Accept", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
////                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
////                        .header("User-Agent", "application/x-www-form-urlencoded")
//                        .contentType("application/x-www-form-urlencoded")
//                        .setFollowRedirects(false)
//                        .form(urlPayMap)
//                        .execute();
//                String loc = executeLocation.header("Location");
//                log.info("yyy location 修正: {}, http status: {}", loc, executeLocation.getStatus());
////                String subBox = qrHtml.substring(qrHtml.indexOf("qrUrl=") + 6);
////                jdPay = subBox.substring(0, subBox.indexOf("\""));
//                log.info(" yyy 修正body: {}", executeLocation.body());
//                jdPay = loc;
//                log.info(" yyy 修正login pay url: {}", jdPay);
//
//                urlPay = URLUtil.url(jdPay);
//                urlPayStringMap = HttpUtil.decodeParamMap(urlPay.getQuery(), StandardCharsets.UTF_8);
//                urlPayMap = new HashMap<>(urlPayStringMap);
//                log.info(" yyy map 2 param: {}", urlPayMap);
//
//                String qrHtml = HttpRequest.post("https://wepay.jd.com/jdpay/login")
////                        .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
////                        .header("Accept", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
////                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
////                        .header("User-Agent", userAgent)
//                        .contentType("application/x-www-form-urlencoded")
//                        .form(urlPayMap)
//                        .setFollowRedirects(false)
//                        .execute().body();
//                String subBox = qrHtml.substring(qrHtml.indexOf("qrUrl=") + 6);
//                jdPay = subBox.substring(0, subBox.indexOf("\""));
//                log.info(" yyy 修正后 pay url: {}", jdPay);
//                payUrl = jdPay;
                return payUrl;
            }

        }

        if ("alipay_mobile".equalsIgnoreCase(data.getString("channel")) || "alipay_qr".equalsIgnoreCase(data.getString("channel"))) {
            log.info("alipay url 初始: {}", resource_url);
            HttpResponse execute = HttpRequest.get(resource_url)
                    .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                                .form(objectObjectSortedMap)
                    .contentType("application/x-www-form-urlencoded")
                    .header("X-Requested-With", "com.seasun.gamemgr")
                    .header("User-Agent", userAgent)
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
                    .header("User-Agent", userAgent)
                    .header("Origin", "https://m.xoyo.com")
                    .header("Referer", "https://m.xoyo.com")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .timeout(5000)
                    .execute();
            String cashier = cashierExecute.header("Location");
            log.info("alipay 修正 后 pay url: {}", cashier);
            payUrl = cashier;
            return payUrl;
        }

        if ("weixin_mobile".equalsIgnoreCase(data.getString("channel"))) {
            log.info("weixin_mobile url 初始: {}", resource_url);

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
                        .header("User-Agent", userAgent)
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
            if (!StringUtils.hasLength(body)) throw new ServiceException("微信端异常，请重新下单");
            if (body.contains("weixin://wap")) {
                String wxPayUrl = body.substring(body.indexOf("weixin://wap"), body.indexOf("&sign=") + 38);
                payUrl = wxPayUrl;
                log.info("wx success : [{}]", wxPayUrl);
                return payUrl;
            }

            log.info("weixin_mobile url 初始: {}", resource_url);
            String prepayId = stringMap.get("prepay_id");
            String pkg = stringMap.get("package");

            String ticketJson = HttpRequest.get("http://localhost:9797?aid=2093769752&proxy=" + ProxyInfoThreadHolder.getIpAddr() + ":" + ProxyInfoThreadHolder.getPort() + "&ua=" + userAgent)
                    .execute().body();
            log.info("weixin_mobile ticket 准备: {}", ticketJson);
            JSONObject ticketObject = JSONObject.parseObject(ticketJson);
            String ticket = ticketObject.getString("ticket");
            String randStr = ticketObject.getString("randstr");

            HttpResponse execute = HttpRequest.get("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkcaptcha?ticket=" + ticket + "&randstr=" + randStr + "&prepayid=" + prepayId + "&package=" + pkg)
                    .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                    .header("User-Agent", userAgent)
                    .header("Referer", resource_url)
                    .execute();
            body = execute.body();
            JSONObject parseObject = JSONObject.parseObject(body);
            String deeplink = parseObject.getString("deeplink");
            log.info("wx 修正 后 pay url: {}", deeplink);
            payUrl = deeplink;
            return payUrl;
        }

        return payUrl;
    }
}
