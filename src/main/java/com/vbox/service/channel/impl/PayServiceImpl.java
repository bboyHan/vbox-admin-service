package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.signers.JWTSignerUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.common.ResultOfList;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.*;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.GoogleAuthenticator;
import com.vbox.common.util.ProxyUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.exception.UnSupportException;
import com.vbox.config.local.PayerInfoThreadHolder;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.*;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.*;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import com.vbox.service.task.DelayTask;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    private UserMapper userMapper;
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
    @Autowired
    private LocationMapper locationMapper;
    @Autowired
    private ChannelShopMapper channelShopMapper;
    @Autowired
    private ChannelPreMapper channelPreMapper;
    @Autowired
    private VboxProxyMapper vboxProxyMapper;
    @Autowired
    private UserAuthMapper userAuthMapper;

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

    public void addProxy(String area, String payIp, String pr) {
//        String body = HttpRequest.get("http://1.14.96.183:8005/server?num=1&Ackey=1h1cf17").execute().body();
//        String body = HttpRequest.get("http://1.14.96.183:8005/server?num=1&Ackey=1h3495e").execute().body();
//        String body = HttpRequest.get("http://api.shenlongip.com/ip?key=tmocdhlc&sign=bdad24bb1b322eebc34be6c35e9c63c7&count=1&area=" + area).execute().body();
//        String body = HttpRequest.get("http://api.shenlongip.com/ip?key=6sbdqwj2&sign=bdad24bb1b322eebc34be6c35e9c63c7&count=1&area=" + area).execute().body();
//        String[] split = body.split(":");
//        int port = Integer.parseInt(split[1].trim());
//        String ipAddr = split[0];
        log.info("传入: area - {}, pay ip - {}, pr - {}", area, payIp, pr);
        String areaCity = null;
        String ipAddr = null;
        String ripAddr = null;
        int port = 0;

        if ("127.0.0.1".equals(payIp)) {
            Set<String> keys = redisUtil.getKeysByPattern(CommonConstant.CHANNEL_PROXY + "*");
            if (!keys.isEmpty()) {
                String randomKey = keys.iterator().next();

                if (redisUtil.isKeyAvailable(randomKey)) {
                    redisUtil.incrementCount(randomKey);
                    // 提取逗号后的IP和端口值
                    String ipPort = randomKey.substring(randomKey.indexOf(",") + 1);
                    String[] addr = ipPort.split(":");

                    ProxyInfo proxyInfo = new ProxyInfo();
                    port = Integer.parseInt(addr[1]);
                    ipAddr = addr[0];
                    proxyInfo.setPort(port);
                    proxyInfo.setIpAddr(ipAddr);

                    log.warn("缓存取 proxy value - {}", proxyInfo);

                    ProxyInfoThreadHolder.addProxy(proxyInfo);

                    CommonUtil.ip2region(ipAddr);
                }else {
                    String bodyRandom = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json").execute().body();
                    log.info("127随机 - 全国area，获取代理 resp: {}", bodyRandom);
                    JSONObject resp = null;
                    try {
                        resp = JSONObject.parseObject(bodyRandom);
                        JSONArray list = resp.getJSONArray("data");
                        JSONObject data = list.getJSONObject(0);
                        ipAddr = data.getString("ip");
                        ripAddr = data.getString("rip");
                        port = data.getInteger("port");
                    } catch (Exception exx) {
                        log.info("127随机 - 代理解析失败 msg: {}", exx.getMessage());
                    }

                    ProxyInfo proxyInfo = new ProxyInfo();
                    proxyInfo.setPort(port);
                    proxyInfo.setIpAddr(ipAddr);

                    log.warn("proxy value - {}", proxyInfo);

                    ProxyInfoThreadHolder.addProxy(proxyInfo);

                    CommonUtil.ip2region(ipAddr);
                    CommonUtil.ip2region(ripAddr);

                    redisUtil.setKey(ipAddr, port);
                }

            } else {
                String bodyRandom = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json").execute().body();
                log.info("127随机 - 全国area，获取代理 resp: {}", bodyRandom);
                JSONObject resp = null;
                try {
                    resp = JSONObject.parseObject(bodyRandom);
                    JSONArray list = resp.getJSONArray("data");
                    JSONObject data = list.getJSONObject(0);
                    ipAddr = data.getString("ip");
                    ripAddr = data.getString("rip");
                    port = data.getInteger("port");
                } catch (Exception exx) {
                    log.info("127随机 - 代理解析失败 msg: {}", exx.getMessage());
                }

                ProxyInfo proxyInfo = new ProxyInfo();
                proxyInfo.setPort(port);
                proxyInfo.setIpAddr(ipAddr);

                log.warn("proxy value - {}", proxyInfo);

                ProxyInfoThreadHolder.addProxy(proxyInfo);

                CommonUtil.ip2region(ipAddr);
                CommonUtil.ip2region(ripAddr);

                redisUtil.setKey(ipAddr, port);
            }


        }else {
            // shenlong
            if (StringUtils.hasLength(payIp)) {
                String location = CommonUtil.ip2region(payIp);
                if (location == null) {
                    throw new ServiceException("传入ip有误，请确认是否为正确Ipv4地址");
                }
                log.info("当前传入ip，查询location为 : {}", location);
                String[] split = location.split("\\|");
                String regionCity = split[3];
                Location locCity = locationMapper.regionSearch(regionCity);
                if (locCity != null) {
                    areaCity = locCity.getArea();
                    log.info("从库里取出【市区】, area : {} ,loc : {}", areaCity, locCity);
                }
                String region = split[2];
                Location loc = locationMapper.regionSearch(region);
                if (loc != null) {
                    area = loc.getArea();
                    log.info("从库里取出【省区】, area : {} ,loc : {}", area, loc);
                }

                if (StringUtils.hasLength(areaCity)) {
                    String bodyCity = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=1&count=1&rip=1&pattern=json&area=" + areaCity).execute().body();
//                String bodyCity = HttpRequest.get("http://ip.quanminip.com/ip?secret=n7VuiYE6&num=1&port=1&type=json&cs=1&mr=1&sign=27ec7a99aa182aa07192281bbcb652d3&region=" + areaCity).execute().body();
                    log.info("1- 市区area传入，获取代理 resp: {}", bodyCity);
                    JSONObject resp = null;
                    try { //市区
                        resp = JSONObject.parseObject(bodyCity);
                        JSONArray list = resp.getJSONArray("data");
                        JSONObject data = list.getJSONObject(0);
                        ipAddr = data.getString("ip");
                        ripAddr = data.getString("rip");
                        port = data.getInteger("port");
                    } catch (Exception e) { //省区
                        log.info("1- 代理解析失败 msg: {}", e.getMessage());
                        if (StringUtils.hasLength(area)) {
                            String body = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json&area=" + area).execute().body();
//                        String body = HttpRequest.get("http://ip.quanminip.com/ip?secret=n7VuiYE6&num=1&port=1&type=json&cs=1&mr=1&sign=27ec7a99aa182aa07192281bbcb652d3&region=" + area).execute().body();
                            log.info("2- 从省区area，获取代理 resp: {}", body);
                            try {
                                resp = JSONObject.parseObject(body);
                                JSONArray list = resp.getJSONArray("data");
                                JSONObject data = list.getJSONObject(0);
                                ipAddr = data.getString("ip");
                                ripAddr = data.getString("rip");
                                port = data.getInteger("port");
                            } catch (Exception ex) {
                                log.info("2- 代理解析失败 msg: {}", ex.getMessage());
                                String bodyRandom = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json").execute().body();
                                log.info("3- 全国area，获取代理 resp: {}", bodyRandom);
                                try {
                                    resp = JSONObject.parseObject(bodyRandom);
                                    JSONArray list = resp.getJSONArray("data");
                                    JSONObject data = list.getJSONObject(0);
                                    ipAddr = data.getString("ip");
                                    ripAddr = data.getString("rip");
                                    port = data.getInteger("port");
                                } catch (Exception exx) {
                                    log.info("3- 代理解析失败 msg: {}", ex.getMessage());
                                }
                            }
                        }

                    }
                } else if (!StringUtils.hasLength(areaCity) && StringUtils.hasLength(area)) {
                    String body = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json&area=" + area).execute().body();
//                String body = HttpRequest.get("http://ip.quanminip.com/ip?secret=n7VuiYE6&num=1&port=1&type=json&cs=1&mr=1&sign=27ec7a99aa182aa07192281bbcb652d3&region=" + area).execute().body();
                    log.info("11- 从省区area，获取代理 resp: {}", body);
                    JSONObject resp = null;
                    try {
                        resp = JSONObject.parseObject(body);
                        JSONArray list = resp.getJSONArray("data");
                        JSONObject data = list.getJSONObject(0);
                        ipAddr = data.getString("ip");
                        ripAddr = data.getString("rip");
                        port = data.getInteger("port");
                    } catch (Exception ex) {
                        log.info("11- 代理解析失败 msg: {}", ex.getMessage());
                        String bodyRandom = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json").execute().body();
//                    String bodyRandom = HttpRequest.get("http://ip.quanminip.com/ip?secret=n7VuiYE6&num=1&port=1&type=json&cs=1&mr=1&sign=27ec7a99aa182aa07192281bbcb652d3").execute().body();
                        log.info("22- 全国area，获取代理 resp: {}", bodyRandom);
                        try {
                            resp = JSONObject.parseObject(bodyRandom);
                            JSONArray list = resp.getJSONArray("data");
                            JSONObject data = list.getJSONObject(0);
                            ipAddr = data.getString("ip");
                            ripAddr = data.getString("rip");
                            port = data.getInteger("port");
                        } catch (Exception exx) {
                            log.info("22- 代理解析失败 msg: {}", exx.getMessage());
                        }
                    }

                } else {
                    String bodyRandom = HttpRequest.get("http://api.shenlongip.com/ip?key=iah0c7fo&sign=94f84cf83d512b135be2a82f9028d353&mr=1&protocol=2&count=1&rip=1&pattern=json").execute().body();
//                String bodyRandom = HttpRequest.get("http://ip.quanminip.com/ip?secret=n7VuiYE6&num=1&port=1&type=json&cs=1&mr=1&sign=27ec7a99aa182aa07192281bbcb652d3").execute().body();
                    log.info("111- 全国area，获取代理 resp: {}", bodyRandom);
                    JSONObject resp = null;
                    try {
                        resp = JSONObject.parseObject(bodyRandom);
                        JSONArray list = resp.getJSONArray("data");
                        JSONObject data = list.getJSONObject(0);
                        ipAddr = data.getString("ip");
                        ripAddr = data.getString("rip");
                        port = data.getInteger("port");
                    } catch (Exception exx) {
                        log.info("222- 代理解析失败 msg: {}", exx.getMessage());
                    }
                }
            }

            ProxyInfo proxyInfo = new ProxyInfo();
            proxyInfo.setPort(port);
            proxyInfo.setIpAddr(ipAddr);

            log.warn("proxy value - {}", proxyInfo);

            ProxyInfoThreadHolder.addProxy(proxyInfo);

            CommonUtil.ip2region(ipAddr);
            CommonUtil.ip2region(ripAddr);


        }


    }

    @Override
    public Object createOrder(OrderCreateExtParam orderCreateExtParam, String area, String pr) throws Exception {
        OrderCreateParam orderCreateParam = new OrderCreateParam();
        BeanUtils.copyProperties(orderCreateExtParam, orderCreateParam);
        orderCreateParam.setMoney(NumberUtil.parseInt(orderCreateExtParam.getMoney()));
        log.info("入参: area - {}, pr - {}, {}", area, pr, orderCreateParam);
        String pa = orderCreateParam.getP_account();
        String orderId = orderCreateParam.getP_order_id();
        //参数校验
        CChannel channel = paramCheckCreateOrder(orderCreateParam);

        // proxy setting
        addProxy(area, orderCreateParam.getPay_ip(), pr);

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
        log.info("资源池取出...{}", randomACInfo);

        PayInfo payInfo = new PayInfo();
        CGatewayInfo cgi = this.cGatewayMapper.getGateWayInfoByCIdAndGId(randomACInfo.getCid(), randomACInfo.getGid());
        payInfo.setChannel(cgi.getCChannel());
//        payInfo.setChannel("weixin_mobile");
        account = randomACInfo.getAcAccount();
        payInfo.setRepeat_passport(account);
        payInfo.setGame(cgi.getCGame());
        payInfo.setGateway(cgi.getCGateway());
        payInfo.setRecharge_unit(reqMoney);
        payInfo.setRecharge_type(6);
        String acPwd = randomACInfo.getAcPwd();
        String cookie = "";
//        boolean ckCheck = gee4Service.tokenCheck(randomACInfo.getCk(), account);
//        if (ckCheck) {
//            cookie = payInfo.getCk();
//        }else {
        cookie = this.getCK(account, Base64.decodeStr(acPwd));
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
//        }

        payInfo.setCk(cookie);
        redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】正在创建订单.... ck 校验成功  ");
        log.info("【商户：" + pa + "】【订单ID：" + orderId + "】正在创建订单.... ck 校验成功  ");

        // 创建订单
        JSONObject orderResp = gee4Service.createOrder(payInfo);

        if (orderResp != null && orderResp.get("data") != null) {
            if (orderResp.getInteger("code") != 1) {
                throw new ServiceException(orderResp.toString());
            } else {

                JSONObject data = orderResp.getJSONObject("data");
                String platform_oid = data.getString("vouch_code");
                String resource_url = data.getString("resource_url");
                PayOrderEvent event = new PayOrderEvent();

//                if ("weixin_mobile".equalsIgnoreCase(data.getString("channel"))) {
//                    URL url = URLUtil.url(resource_url);
//                    Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), null);
//                    String redirect_url = URLDecoder.decode(stringMap.get("redirect_url"), "utf-8");
//                    stringMap.put("redirect_url", redirect_url);
//                    Map<String, Object> objectObjectSortedMap = new HashMap<>(stringMap);
//                    String body = null;
//                    try {
//                        HttpResponse execute = HttpRequest.post("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb")
//                                .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
//                                .form(objectObjectSortedMap)
//                                .contentType("application/x-www-form-urlencoded")
//                                .header("X-Requested-With", "com.seasun.gamemgr")
//                                .header("Origin", "https://m.xoyo.com")
//                                .header("Referer", "https://m.xoyo.com")
//                                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                                .timeout(5000)
//                                .execute();
//                        body = execute.body();
//                    } catch (HttpException e) {
//                        e.printStackTrace();
//                        throw new ServiceException("微信端异常，请重新下单");
//                    }
//                    log.info("wx success");
//                    if (!StringUtils.hasLength(body)) throw new ServiceException("微信端异常，请重新下单");
//                    event.setExt(body);
//                }

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

                String envProxyUrl = vboxProxyMapper.getEnvUrl("loc");

                String h5Url = envProxyUrl + CommonConstant.ENV_HOST_PAY_URL + orderId;
//                String h5Url = "http://mng.vboxjjjxxx.info/#/code/pay?orderId=" + orderId;
                payOrder.setResourceUrl(payUrl);
                payOrder.setNotifyUrl(orderCreateParam.getNotify_url());
                payOrder.setOrderStatus(OrderStatusEnum.NO_PAY.getCode());
                payOrder.setCallbackStatus(OrderCallbackEnum.NOT_CALLBACK.getCode());
                payOrder.setCodeUseStatus(CodeUseStatusEnum.FINISHED.getCode());
                payOrder.setCreateTime(nowTime);
                pOrderMapper.insert(payOrder);

                event.setOrderId(orderId);
                event.setEventLog(data.toJSONString());
                event.setPlatformOid(platform_oid);
                event.setCreateTime(nowTime);

                pOrderEventMapper.insert(event);

                DelayTask<PayOrder> delayTask = new DelayTask<>();
                delayTask.setId(IdUtil.randomUUID());
                delayTask.setTaskName("order_delay_" + platform_oid);
                delayTask.setTask(payOrder);
                boolean delaySetting = redisUtil.zAdd("order_delay_queue", delayTask, 300000L);
                if (delaySetting) {
                    log.info("delay info: {}, expire time: {}", delayTask, "5min");
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

            URL url = URLUtil.url(resource_url);
            Map<String, String> stringMap = HttpUtil.decodeParamMap(url.getQuery(), null);
            String prepayId = stringMap.get("prepay_id");
            String pkg = stringMap.get("package");

            String ticketJson = HttpRequest.get("http://localhost:9797?aid=2093769752&proxy=" + ProxyInfoThreadHolder.getIpAddr() + ":" + ProxyInfoThreadHolder.getPort())
                    .execute().body();
            JSONObject ticketObject = JSONObject.parseObject(ticketJson);
            String ticket = ticketObject.getString("ticket");
            String randStr = ticketObject.getString("randstr");

            HttpResponse execute = HttpRequest.get("https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkcaptcha?ticket=" + ticket + "&randstr=" + randStr + "&prepayid=" + prepayId + "&package=" + pkg)
                    .execute();
            String body = execute.body();
            JSONObject parseObject = JSONObject.parseObject(body);
            String deeplink = parseObject.getString("deeplink");
            log.info("wx 修正 后 pay url: {}", deeplink);
            payUrl = deeplink;
        } else {
            payUrl = resource_url;
        }

        return payUrl;
    }

    public void removeSdoElements(List<ChannelPre> sdoList) {
        Iterator<ChannelPre> iterator = sdoList.iterator();
        LocalDateTime now = LocalDateTime.now();

        while (iterator.hasNext()) {
            ChannelPre pre = iterator.next();
            LocalDateTime createTime = pre.getCreateTime();
            LocalDateTime pre30min = now.plusDays(-1);
            if (createTime.isBefore(pre30min)) { //当前预产时间已经是一天前的, remove并置为超时状态
                channelPreMapper.updateByPlatId(pre.getPlatOid(), 3);
                iterator.remove();
            }
        }
    }

    public CChannel paramCheckCreateOrder(OrderCreateParam orderCreateParam) {
//        String pa = orderCreateParam.getP_account();
//        String sign = orderCreateParam.getSign();
//        PAccount paDB = pAccountMapper.selectOne((new QueryWrapper<PAccount>()).eq("p_account", pa));
//        String pKey = paDB.getPKey();
//        orderCreateParam.setSign((String) null);
//        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);
//        String signDB = CommonUtil.encodeSign(map, pKey);
//        if (!signDB.equals(sign)) {
//            throw new ValidateException("入参仅限文档包含字段，请核对");
//        } else {
        String channelId = orderCreateParam.getChannel_id();
        CChannel channel = channelMapper.getChannelByChannelId(channelId);
        if (channel == null) {
            throw new UnSupportException("通道id错误，请重新查询确认");
        } else {
            String notify = orderCreateParam.getNotify_url();
            boolean isUrl = CommonUtil.isUrl(notify);
            if (!isUrl) {
                throw new UnSupportException("notify_url不合法，请检验入参");
            } else {
                String attach = orderCreateParam.getAttach();
                Integer money = orderCreateParam.getMoney();
                if (channelId.equals("jx3_ali_gift") || channelId.equals("jx3_wx_gift")) {
                    Integer type = JXHTEnum.type(money);
                    if (type == -1) {
                        throw new UnSupportException("该通道属于固额设置，请检验入参，金额限定为76，156，162，276");
                    }
                }
                if (channelId.contains("tx")) {
                    if (channelId.equals("tx_jym")) {
                        log.warn("jym 金额不设定，正常通过");
                    } else {
                        QueryWrapper<ChannelShop> queryWrapper = new QueryWrapper<>();
                        queryWrapper.eq("channel", channelId);
                        queryWrapper.eq("money", money);
                        queryWrapper.eq("status", 1);
                        List<ChannelShop> randomTempList = channelShopMapper.selectList(queryWrapper);
                        if (randomTempList != null && randomTempList.size() != 0) {
                            log.warn("tx通道库存金额校验通过 >> channel: {}, money: {}", channel, money);
                        } else {
                            List<Integer> checkTempList = channelShopMapper.getChannelShopMoneyList(channelId);
                            if (checkTempList != null && checkTempList.size() != 0) {
                                throw new UnSupportException("该通道属于固额设置，请检验入参，金额限定为" + checkTempList);
                            } else {
                                throw new UnSupportException("该通道无库存金额，请联系管理员");
                            }
                        }
                    }
                }
                if (channelId.contains("sdo")) {
                    if (money == 200) {
                        money = 204;
                    } else if (money == 1) {
                        money = 1;
                    } else if (money == 100) {
                        money = 102;
                    } else {
                        throw new UnSupportException("仅支持100、200的固额设置");
                    }

                    QueryWrapper<ChannelPre> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("status", 2);
                    queryWrapper.eq("money", money);

                    List<ChannelPre> channelPres = channelPreMapper.selectList(queryWrapper);
                    removeSdoElements(channelPres);

                    if (channelPres.size() == 0) {
                        log.error("库存金额不足");
                        throw new UnSupportException("该通道无库存金额，请联系管理员");
                    }
                }

//                if (channelId.equals("tx_dy") || channelId.equals("tx_jd")) {
//                    List<Integer> moneyList = Arrays.asList(50, 100, 200, 300, 500);
//                    if (!moneyList.contains(orderCreateParam.getMoney())) {
//                        throw new ValidateException("该通道属于固额设置，请检验入参，金额限定为" + moneyList);
//                    }
//                }
//                if (channelId.equals("tx_tb")) {
//                    List<Integer> moneyList = Arrays.asList(30, 50, 100, 200, 300);
//                    if (!moneyList.contains(orderCreateParam.getMoney())) {
//                        throw new ValidateException("该通道属于固额设置，请检验入参，金额限定为" + moneyList);
//                    }
//                }
//                if (channelId.equals("tx_zfb")) {
//                    List<Integer> moneyList = Arrays.asList(50, 100);
//                    if (!moneyList.contains(orderCreateParam.getMoney())) {
//                        throw new ValidateException("该通道属于固额设置，请检验入参，金额限定为" + moneyList);
//                    }
//                }
                if (attach != null && attach.length() > 128) {
                    throw new UnSupportException("attach不合法，请检验入参");
                } else {
                    String orderId = orderCreateParam.getP_order_id();
                    if (orderId != null && orderId.length() <= 32 && orderId.length() >= 12) {
                        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
                        if (poDB != null) {
                            throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
                        } else {
                            return channel;
                        }
                    } else {
                        throw new UnSupportException("orderId不合法，请检验入参");
                    }
                }
            }
        }
//        }
    }

    @Override
    public Object createTestOrder(Integer num, String acid, String channel, String area, String pr, String payIp) throws Exception {
        OrderCreateParam orderCreateParam = new OrderCreateParam();
        String orderId = IdUtil.simpleUUID();
        orderCreateParam.setP_order_id(orderId);
        orderCreateParam.setMoney(num);

        String envProxyUrl = vboxProxyMapper.getEnvUrl("loc");

        orderCreateParam.setNotify_url(envProxyUrl + "/basic-api/test/callback");
//        orderCreateParam.setNotify_url("http://mng.vboxjjjxxx.info/basic-api/test/callback");
        orderCreateParam.setChannel_id(channel);
        orderCreateParam.setAcid(acid);
        orderCreateParam.setPay_ip(payIp);
        orderCreateParam.setP_account("e191aa33c9a74416b6ae6aa66d7195f1");
        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);
        String sign = CommonUtil.encodeSign(map, "00b79aa26d6f412984c8926300427e39");
        orderCreateParam.setSign(sign);
        OrderCreateExtParam orderCreateExtParam = new OrderCreateExtParam();
        BeanUtils.copyProperties(orderCreateParam, orderCreateExtParam);
        orderCreateExtParam.setMoney(orderCreateParam.getMoney() + "");
        return createAsyncOrder(orderCreateExtParam, area, pr);
    }

    @Override
    public Object createAsyncOrder(OrderCreateExtParam orderCreateExtParam, String area, String pr) throws Exception {
        log.info("入参: area - {}, pr - {}, {}", area, pr, orderCreateExtParam);

        String pa = orderCreateExtParam.getP_account();
        String sign = orderCreateExtParam.getSign();
        PAccount paDB = pAccountMapper.selectOne((new QueryWrapper<PAccount>()).eq("p_account", pa));
        String pKey = paDB.getPKey();
        orderCreateExtParam.setSign(null);
        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateExtParam);
        String signDB = CommonUtil.encodeSign(map, pKey);
        if (!signDB.equals(sign)) {
            throw new UnSupportException("入参仅限文档包含字段，请核对sign值计算规则");
        }

        OrderCreateParam orderCreateParam = new OrderCreateParam();
        BeanUtils.copyProperties(orderCreateExtParam, orderCreateParam);
        orderCreateParam.setMoney(NumberUtil.parseInt(orderCreateExtParam.getMoney()));

        log.info("修正参: area - {}, pr - {}, {}", area, pr, orderCreateParam);
        String orderId = orderCreateParam.getP_order_id();
        //参数校验
        CChannel channel = paramCheckCreateOrder(orderCreateParam);

        Integer reqMoney = orderCreateParam.getMoney();
        log.info("create order 创建订单: {}, p account: {}, 金额: {}", orderId, pa, reqMoney);
        LocalDateTime nowTime = LocalDateTime.now();
        String orderKey = "redisLock_order:" + orderId;
        if (redisUtil.hasKey(orderKey)) {
            throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
        }
        redisUtil.set(orderKey, 1, 300L);
        /*CAccountInfo randomACInfo = new CAccountInfo();
        String account;
        String now;
        if (null == orderCreateParam.getAcid()) {
            List<CAccountInfo> cAccountList = cAccountMapper.listCanPayForCAccount();
            if (cAccountList == null || cAccountList.size() == 0) {
                throw new NotFoundException("系统不可用充值渠道，请联系管理员");
            }

            String orderKey = "redisLock_order:" + orderId;
            if (redisUtil.hasKey(orderKey)) {
                throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
            }

            redisUtil.set(orderKey, 1, 300L);
            log.info("create order 创建订单: {}, p account: {}", orderId, pa);
            redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】正在创建订单....  ");
            reqMoney = orderCreateParam.getMoney();
            nowTime = LocalDateTime.now();
            now = DateUtil.format(nowTime, "yyyy-MM-dd");
            List<CAccountInfo> cAccountListToday = cAccountMapper.listCanPayForCAccountToday(now);
            for (CAccountInfo c : cAccountListToday) {
                c.setCreateTime(nowTime);
            }

            List<CAccountInfo> randomTemp = computeAsync(channel.getId(), now, cAccountList, cAccountListToday);
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
        log.info("资源池取出...randomACInfo - {}", randomACInfo);

        PayInfo payInfo = new PayInfo();
        CGatewayInfo cgi = cGatewayMapper.getGateWayInfoByCIdAndGId(randomACInfo.getCid(), randomACInfo.getGid());
        payInfo.setChannel(cgi.getCChannel());
        account = randomACInfo.getAcAccount();
        payInfo.setRepeat_passport(account);
        payInfo.setGame(cgi.getCGame());
        payInfo.setGateway(cgi.getCGateway());
        payInfo.setRecharge_unit(reqMoney);
        payInfo.setRecharge_type(6);*/

//        redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】创建订单准备.... ck 校验成功  ");
//        log.info("【商户：" + pa + "】【订单ID：" + orderId + "】创建订单准备.... ck 校验成功  ");
        //
//        String acId = randomACInfo.getAcid();
        String cChannelId = channel.getCChannelId();
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setPAccount(pa);
        payOrder.setCost(reqMoney);
//        payOrder.setAcId(acId);
//        payOrder.setPayIp(orderCreateParam.getPay_ip());
        payOrder.setCChannelId(orderCreateParam.getChannel_id());

        String envProxyUrl = vboxProxyMapper.getEnvUrl("loc");

        String h5Url = envProxyUrl + CommonConstant.ENV_HOST_PAY_URL + orderId;
        payOrder.setNotifyUrl(orderCreateParam.getNotify_url());
        payOrder.setOrderStatus(OrderStatusEnum.PAY_CREATING.getCode()); //4 - 创建中
        payOrder.setCallbackStatus(OrderCallbackEnum.NOT_CALLBACK.getCode());
        payOrder.setCodeUseStatus(CodeUseStatusEnum.NO_USE.getCode());
        payOrder.setCreateTime(nowTime);
        pOrderMapper.insert(payOrder);

        PayOrderEvent event = new PayOrderEvent();
        event.setOrderId(orderId);
        event.setCreateTime(nowTime);
        pOrderEventMapper.insert(event);

//        POrderQueue pOrderQueue = new POrderQueue();
//        pOrderQueue.setPa(pa);
//        pOrderQueue.setChannel(channel.getId());
//        pOrderQueue.setOrderId(orderId);
//        pOrderQueue.setPayIp(orderCreateParam.getPay_ip());
//        pOrderQueue.setReqMoney(reqMoney);
//        pOrderQueue.setArea(area);
//        pOrderQueue.setAcid(acId);
//        pOrderQueue.setPr(pr);
//        redisUtil.lPush(CommonConstant.ORDER_CREATE_QUEUE, pOrderQueue);

        PayOrderCreateVO p = new PayOrderCreateVO();
        p.setPayUrl(h5Url);
        p.setOrderId(orderId);
        p.setCost(reqMoney);
        p.setAttach(orderCreateParam.getAttach());
        p.setStatus(4);
        p.setChannelId(orderCreateParam.getChannel_id());
//        redisUtil.pub("【商户：" + pa + "】【订单ID：" + orderId + "】创建异步订单准备工作完成.... 付款链接: " + h5Url);
        log.info("【商户：" + pa + "】【订单ID：" + orderId + "】创建订单准备工作完成.... 付款链接: " + h5Url);
        return p;

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
    public Object listOrderWait(OrderQueryParam queryParam) {

        QueryWrapper<PayOrder> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(queryParam.getP_account())) {
            queryWrapper.eq("p_account", queryParam.getP_account());
        }
        if (StringUtils.hasLength(queryParam.getOrderId())) {
            queryWrapper.likeRight("order_id", queryParam.getOrderId());
        }

        if (StringUtils.hasLength(queryParam.getCallbackStatus())) {
            queryWrapper.eq("callback_status", queryParam.getCallbackStatus());
        }
        if (StringUtils.hasLength(queryParam.getCChannelId())) {
            queryWrapper.eq("c_channel_id", queryParam.getCChannelId());
        }

        queryWrapper.eq("order_status", 4);
        queryWrapper.orderByDesc("id");

        Page<PayOrder> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 20);
        }

        Page<PayOrder> payOrderPage = pOrderMapper.selectPage(page, queryWrapper);
        List<PayOrder> payOrders = payOrderPage.getRecords();
        List<PayOrderVO> voList = new ArrayList<>(payOrders.size());
        for (PayOrder p : payOrders) {
            PayOrderVO target = new PayOrderVO();
            BeanUtils.copyProperties(p, target);
            target.setPa(p.getPAccount());

            CAccount ca = cAccountMapper.getCAccountByAcid(p.getAcId());
            target.setAcRemark(ca == null ? null : ca.getAcRemark());
            target.setAcAccount(ca == null ? null : ca.getAcAccount());
            target.setChannel(ChannelEnum.of(p.getCChannelId()));
            target.setCallbackStatus(p.getCallbackStatus());
            voList.add(target);
        }

        ResultOfList rs = new ResultOfList(voList, (int) page.getTotal());

        return rs;
    }

    /**
     * 查询账户自己的所有订单
     */
    @Override
    public Object listOrder(OrderQueryParam queryParam) {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        List<String> acIdList = cAccountMapper.listAcIdInUids(sidList);
        if (acIdList.size() == 0) return new ArrayList<>();
        QueryWrapper<PayOrder> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasLength(queryParam.getP_account())) {
            queryWrapper.eq("p_account", queryParam.getP_account());
        }
        if (StringUtils.hasLength(queryParam.getAcAccount()) && !StringUtils.hasLength(queryParam.getAcRemark())) {
            List<CAccount> acList = cAccountMapper.selectList(new QueryWrapper<CAccount>().like("ac_account", queryParam.getAcAccount()));
            acIdList = new ArrayList<>();
            for (CAccount cAccount : acList) {
                String acid = cAccount.getAcid();
                acIdList.add(acid);
            }
            queryWrapper.in("ac_id", acIdList);
        } else if (!StringUtils.hasLength(queryParam.getAcAccount()) && StringUtils.hasLength(queryParam.getAcRemark())) {
            List<CAccount> acList = cAccountMapper.selectList(new QueryWrapper<CAccount>().like("ac_remark", queryParam.getAcRemark()));
            acIdList = new ArrayList<>();
            for (CAccount cAccount : acList) {
                String acid = cAccount.getAcid();
                acIdList.add(acid);
            }
            queryWrapper.in("ac_id", acIdList);
        } else {
            queryWrapper.in("ac_id", acIdList);
        }
        if (StringUtils.hasLength(queryParam.getOrderId())) {
            queryWrapper.likeRight("order_id", queryParam.getOrderId());
        }
        if (StringUtils.hasLength(queryParam.getOrderStatus())) {
            queryWrapper.eq("order_status", queryParam.getOrderStatus());
        }
        if (StringUtils.hasLength(queryParam.getCallbackStatus())) {
            queryWrapper.eq("callback_status", queryParam.getCallbackStatus());
        }
        if (StringUtils.hasLength(queryParam.getCChannelId())) {
            queryWrapper.eq("c_channel_id", queryParam.getCChannelId());
        }
        queryWrapper.orderByDesc("id");

        Page<PayOrder> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 20);
        }

//        List<PayOrder> payOrders = pOrderMapper.selectList(queryWrapper);
        Page<PayOrder> payOrderPage = pOrderMapper.selectPage(page, queryWrapper);
        List<PayOrder> payOrders = payOrderPage.getRecords();
        List<PayOrderVO> voList = new ArrayList<>(payOrders.size());
        for (PayOrder p : payOrders) {
            PayOrderVO target = new PayOrderVO();
            BeanUtils.copyProperties(p, target);
            target.setPa(p.getPAccount());

            CAccount ca = cAccountMapper.getCAccountByAcid(p.getAcId());
            Integer uid = ca.getUid();
            User user = userMapper.selectById(uid);
            target.setUsername(user.getAccount());
            target.setAcRemark(ca.getAcRemark());
            target.setAcAccount(ca.getAcAccount());
            target.setChannel(ChannelEnum.of(p.getCChannelId()));
            target.setCallbackStatus(p.getCallbackStatus());
            voList.add(target);
        }

        ResultOfList rs = new ResultOfList(voList, (int) page.getTotal());

        return rs;
    }

    @Override
    public long orderCallback(OrderCallbackParam callbackParam) throws Exception {
        // 1. 校验商户
        PayerInfo payerLocal = PayerInfoThreadHolder.getPayerInfo();
        String pa = callbackParam.getP_account();
        String pub = callbackParam.getP_key();
        boolean valid = PayerInfo.valid(payerLocal, new PayerInfo(pub, pa));
        if (!valid) throw new UnSupportException("Token valid");

        String orderId = callbackParam.getP_order_id();
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        if (po == null) throw new NotFoundException("订单不存在");

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
        if (!signDB.equals(sign)) throw new UnSupportException("Token valid");

        String orderId = orderCreateParam.getP_order_id();
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        if (po == null) throw new NotFoundException("订单不存在");
        if (po.getOrderStatus() == 4) {
            PayOrder pov = pOrderMapper.getPOrderByOid(orderId);
            OrderQueryVO vo = new OrderQueryVO();
            vo.setStatus(2);
//        vo.setPayUrl(pov.getResourceUrl());
            String envProxyUrl = vboxProxyMapper.getEnvUrl("loc");
            vo.setPayUrl(envProxyUrl + CommonConstant.ENV_HOST_PAY_URL + pov.getOrderId());
            vo.setCost(pov.getCost());
            vo.setOrderId(pov.getOrderId());
            vo.setNotifyUrl(pov.getNotifyUrl());
            return vo;
        }

        /*JSONObject data = null;
        CAccountWallet wallet = cAccountWalletMapper.selectOne(new QueryWrapper<CAccountWallet>().eq("oid", orderId));
        Integer code;
        JSONObject resp;
        boolean flag = false;

        if (wallet != null) {
            code = 2;
            log.info("商户查单时发现该订单已支付入库, info: {}", wallet);
        } else {
            resp = this.queryOrderForQuery(orderId);
            data = resp.getJSONObject("data");
            code = data.getInteger("order_status");
            flag = true;
        }

        if (po.getOrderStatus() == 3) {
            if (!flag) {
                resp = this.queryOrderForQuery(orderId);
                data = resp.getJSONObject("data");
                code = data.getInteger("order_status");
            }
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
*/
        PayOrder pov = pOrderMapper.getPOrderByOid(orderId);
        OrderQueryVO vo = new OrderQueryVO();
        vo.setStatus(pov.getOrderStatus());
//        vo.setPayUrl(pov.getResourceUrl());

        String envProxyUrl = vboxProxyMapper.getEnvUrl("loc");

        vo.setPayUrl(envProxyUrl + CommonConstant.ENV_HOST_PAY_URL + pov.getOrderId());
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

    public JSONObject queryOrderForQuery(String orderId) throws Exception {

        PayOrderEvent poe = pOrderEventMapper.getPOrderEventByOid(orderId);
        if (poe == null) throw new NotFoundException("订单不存在");
        String pid = poe.getPlatformOid();

        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        CAccount ca = cAccountMapper.getCAccountByAcid(po.getAcId());

        String cookie = getCKforQuery(ca.getAcAccount(), Base64.decodeStr(ca.getAcPwd()));

        boolean expire = gee4Service.tokenCheck(cookie, ca.getAcAccount());
        if (!expire) {
            CAccount cAccount = new CAccount();
            cAccount.setId(ca.getId());
            cAccount.setSysStatus(0);
            cAccount.setSysLog("ck已过期，请及时更新");
            cAccountMapper.updateById(cAccount);
            throw new NotFoundException("ck过期，请联系运营更新后可查看订单");
        }

        SecCode secCode = gee4Service.verifyGeeCapForQuery();

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
            log.warn("缓存池取出 ck, {}", v);
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
        log.info("filePath ： {}", filePath);
        File inputFile = new File(filePath);
        InputStream is = Files.newInputStream(inputFile.toPath());
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);   // 执行指定脚本
        HttpResponse resp = null;
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String payload = (String) invoke.invokeFunction("test", acPwd);
                String encode = URLEncoder.encode(payload, "UTF-8");
//                System.out.println("pwd = " + encode);

                SecCode secCode = gee4Service.capSecCode();

                resp = HttpRequest.get("https://pf-api.xoyo.com/passport/common_api/login")
                        .setHttpProxy(ProxyInfoThreadHolder.getIpAddr(), ProxyInfoThreadHolder.getPort())
                        .form("account", acAccount)
                        .form("encrypt_method", "rsa")
                        .form("captcha_id", secCode.getCaptcha_id())
                        .form("lot_number", secCode.getLot_number())
                        .form("pass_token", secCode.getPass_token())
                        .form("gen_time", secCode.getGen_time())
                        .form("captcha_output", URLEncoder.encode(secCode.getCaptcha_output(), "UTF-8"))
                        .form("password", encode)
//                                .form("callback", "jsonp_ef2891abd4b000")
                        .form("callback", "jsonp_" + RandomUtil.randomNumbers(14))
                        .execute();

                String jsonResp = Gee4Service.parseGeeJson(resp.body());
//                log.info(JSONObject.toJSONString(resp.headers()));
//                log.info(JSONObject.toJSONString(resp.body()));
                List<String> setCkList = resp.headerList("Set-Cookie");
                for (String ck : setCkList) {
                    if (ck.toLowerCase().contains("xoyokey")) {
                        cookie = ck;
                    }
                }

                JSONObject obj = JSONObject.parseObject(jsonResp);
                log.info("login ---- obj: {}", obj);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (Exception e) {
            log.error("/common_api/login, err resp : {}", resp == null ? "无响应数据" : resp.body());
            throw new RuntimeException(e);
        }

        redisUtil.set(CommonConstant.ACCOUNT_CK + acAccount, cookie, 7200); //2hour
        log.info("login ---- ck: {}", cookie);

        return cookie;
    }

    public String getBalance(String ck, String acPwd) throws IOException {
//        Object v = redisUtil.get(CommonConstant.ACCOUNT_CK + acAccount);
//        if (v != null) {
//            log.info("redis中取出ck, v : {}", v);
//            return v.toString();
//        }
//        String cookie = null;

        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
//        File file = ResourceUtils.getFile("classpath:d4.js");
//        ClassPathResource classPathResource = new ClassPathResource("d4.js");
//        InputStream is = classPathResource.getInputStream();
        String property = System.getProperty("user.dir");
        String filePath = (property + File.separator + "d4.js");
//        log.info("filePath ： {}", filePath);
        File inputFile = new File(filePath);
        InputStream is = Files.newInputStream(inputFile.toPath());
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);   // 执行指定脚本
        HttpResponse resp = null;
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String payload = (String) invoke.invokeFunction("test", acPwd);
                String encode = URLEncoder.encode(payload, "UTF-8");
//                System.out.println("pwd = " + encode);

                SecCode secCode = gee4Service.capSecCodeForQuery();

                resp = HttpRequest.get("https://pay-pf-api.xoyo.com/pay/query_api/get_balance")
                        .form("product", "jx3")
                        .form("gateway", "z05")
                        .form("password", encode)
                        .form("encrypt_method", "rsa")
                        .form("captcha_id", secCode.getCaptcha_id())
                        .form("lot_number", secCode.getLot_number())
                        .form("pass_token", secCode.getPass_token())
                        .form("gen_time", secCode.getGen_time())
                        .form("captcha_output", secCode.getCaptcha_output())
                        .form("callback", "jsonp_" + RandomUtil.randomNumbers(14))
                        .cookie(ck)
                        .execute();

                String jsonResp = Gee4Service.parseGeeJson(resp.body());

                JSONObject obj = JSONObject.parseObject(jsonResp);
                log.info("get_balance ---- obj: {}", obj);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (Exception e) {
            log.error("query_api/get_balance, err resp : {}", resp == null ? "无响应数据" : resp.body());
            throw new RuntimeException(e);
        }

        return null;
    }

    public String getCKforQuery(String acAccount, String acPwd) throws IOException {
        Object v = redisUtil.get(CommonConstant.ACCOUNT_CK + acAccount);
        if (v != null) {
            log.info("redis中取出ck, v : {}", v);
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
//        log.info("filePath ： {}", filePath);
        File inputFile = new File(filePath);
        InputStream is = Files.newInputStream(inputFile.toPath());
        File file = new File("tmp");
        CommonUtil.inputStreamToFile(is, file);
        FileReader reader = new FileReader(file);   // 执行指定脚本
        HttpResponse resp = null;
        try {
            engine.eval(reader);
            if (engine instanceof Invocable) {
                Invocable invoke = (Invocable) engine;    // 调用merge方法，并传入两个参数

                String payload = (String) invoke.invokeFunction("test", acPwd);
                String encode = URLEncoder.encode(payload, "UTF-8");
//                System.out.println("pwd = " + encode);

                SecCode secCode = gee4Service.capSecCodeForQuery();

                resp = HttpRequest.get("https://pf-api.xoyo.com/passport/common_api/login")
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
                List<String> ckList = resp.headerList("Set-Cookie");
                for (String ckTarget : ckList) {
                    if (ckTarget.contains("xoyokey")) {
                        cookie = ckTarget;
                    }
                }

                JSONObject obj = JSONObject.parseObject(jsonResp);
                log.info("login 取出ck ---- obj: {}", obj);
//                System.out.println(obj);
//                System.out.println(data);
            }
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (Exception e) {
            log.error("/common_api/login, err resp : {}", resp == null ? "无响应数据" : resp.body());
            throw new RuntimeException(e);
        }

        redisUtil.set(CommonConstant.ACCOUNT_CK + acAccount, cookie, 7200); //2hour
        log.info("login 取出ck ---- ck: {}", cookie);

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

        pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());

        pOrderMapper.updateCallbackStatusByOId(orderId);

        log.info("测试回调商户, 回调请求消息：notify：\n{}，req body：\n{}", payOrder.getNotifyUrl(), reqBody);
        HttpResponse resp = HttpRequest.post(payOrder.getNotifyUrl())
                .body(reqBody)
                .execute();

        log.info("测试回调商户，商户返回信息： http status：\n {}， resp： \n{}", resp.getStatus(), resp.body());
        return resp.body();
    }

    @Override
    public String callbackOrder(String orderId, String captcha) throws IllegalAccessException {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        PayOrder payOrder = pOrderMapper.getPOrderByOid(orderId);

        String pa = payOrder.getPAccount();
        PAccount paDB = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", pa));
        String pKey = paDB.getPKey();
        CAccount caDB = cAccountMapper.getCAccountByAcid(payOrder.getAcId());

        Set<String> roleList = roleMapper.listRoleValueByUid(uid);
        for (String roleValue : roleList) {
            if (!roleValue.contains("mng_sale")) {
                throw new ServiceException("当前账号无操作权限");
            }
        }
        UserAuth userAuth = userAuthMapper.getAuthByUid(uid);
        Boolean googleFlag = GoogleAuthenticator.authcode(captcha, userAuth.getCap());
        if (!googleFlag) throw new ServiceException("验证码错误，请联系谷歌验证码管理人获取！");

        PayNotifyVO payNotifyVO = new PayNotifyVO();
        payNotifyVO.setOrder_id(payOrder.getOrderId());
        payNotifyVO.setCost(payOrder.getCost());
        payNotifyVO.setStatus(1);
        payNotifyVO.setP_account(pa);
        String sign = CommonUtil.encodeSign(CommonUtil.objToTreeMap(payNotifyVO), pKey);
        payNotifyVO.setSign(sign);

        String reqBody = JSONObject.toJSONString(payNotifyVO);

        pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());

        pOrderMapper.updateCallbackStatusByOId(orderId);
        try {
//            CAccount caDB = cAccountMapper.getCAccountByAcid(payOrder.getAcId());
            CAccountWallet w = new CAccountWallet();
            w.setCaid(caDB.getId());
            w.setCost(payOrder.getCost());
            w.setOid(payOrder.getOrderId());
            w.setCreateTime(LocalDateTime.now());
            cAccountWalletMapper.insert(w);
        } catch (Exception var14) {
            log.warn("CAccountWallet 已经入库, err: {}", var14.getMessage());
        }
        log.info("[强补] , 入回调池: orderId - {}", orderId);

        long rowRedis = redisUtil.sSetAndTime(CommonConstant.ORDER_CALLBACK_QUEUE, 300, orderId);

        log.info("强补回调商户, 回调请求消息：\n notify：{}，\n req body：{}", payOrder.getNotifyUrl(), reqBody);
        HttpResponse resp = HttpRequest.post(payOrder.getNotifyUrl())
                .body(reqBody)
                .execute();

        log.info("强补回调商户，商户返回信息：\n http status： {}， \nresp： {}", resp.getStatus(), resp.body());
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
        if (payOrder.getCChannelId().contains("tx")) {
            String platformOid = payOrder.getPlatformOid();
            String QQ = null;
            try {
                String[] split = platformOid.split("\\|");
                QQ = split[2];
                platformOid = QQ;
            } catch (Exception e) {
                platformOid = "QQ|order waiting";
            }
            payOrderCreateVO.setPlatformOid(platformOid);
        }
        return payOrderCreateVO;
    }

    public OrderQueryVO queryAndCallback(String orderId) throws Exception {
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        if (po == null) {
            throw new NotFoundException("订单不存在，请核对");
        } else {
            String cChannelId = po.getCChannelId();
            if (cChannelId.contains("jx3")) {
                PAccount paDB = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", po.getPAccount()));
                JSONObject data = null;
                CAccountWallet wallet = cAccountWalletMapper.selectOne((new QueryWrapper<CAccountWallet>()).eq("oid", orderId));
                Integer code;
                JSONObject resp;
                boolean flag = false;
                if (wallet != null) {
                    code = 2;
                    log.info("手动查单时发现该订单已支付入库, info: {}", wallet);
                } else {
                    resp = queryOrderForQuery(orderId);
                    data = resp.getJSONObject("data");
                    code = data.getInteger("order_status");
                    flag = true;
                }

                if (po.getOrderStatus() == 3) {
                    if (!flag) {
                        resp = queryOrderForQuery(orderId);
                        data = resp.getJSONObject("data");
                        code = data.getInteger("order_status");
                    }

                    if (code == 2) {
                        pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
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
                        log.info("系统手动查单,手动回调请求消息：notify：{}，req body：{}", notify, reqBody);
                        execute = HttpRequest.post(notify).body(reqBody).execute();
                        log.info("系统手动查单,手动回调返回信息： http status： {}， resp： {}", execute.getStatus(), execute.body());
                        if (execute.getStatus() == 200) {
                            pOrderMapper.updateCallbackStatusByOId(orderId);
                            redisUtil.setRemove("order_callback_queue", orderId);
                            log.info("系统手动查单,该订单已回调成功，通知url：{}，orderID：{}", notify, orderId);
                        }
                    } catch (Exception var14) {
                        log.error("系统手动查单,手动回调失败，notify: {}, resp: {}, err: {}", notify, execute, var14);
                    }
                }

                if (code == 2 && wallet == null) {
                    int row = pOrderMapper.updateOStatusByOId(orderId, OrderStatusEnum.PAY_FINISHED.getCode(), CodeUseStatusEnum.FINISHED.getCode());
                    if (row == 1) {
                        CAccount ca = this.cAccountMapper.getCAccountByAcid(po.getAcId());
                        CAccountWallet w = new CAccountWallet();
                        w.setCaid(ca.getId());
                        w.setCost(po.getCost());
                        w.setOid(po.getOrderId());
                        w.setCreateTime(LocalDateTime.now());

                        try {
                            cAccountWalletMapper.insert(w);
                        } catch (Exception ex) {
                            log.warn("已经入库了，{}", ex.getMessage());
                        }

                        log.info("系统手动查单, 查询到该单在平台已支付成功，自动入库并入回调池: orderId - {}， 平台数据：{}", po.getOrderId(), data);
                        long rowRedis = redisUtil.sSet("order_callback_queue", orderId);
                        if (rowRedis == 1L) {
                            log.info("系统手动查单, 查询未支付订单已完成支付，入回调通知池， 订单ID: {}", orderId);
                        }
                    }
                }

            } else {
//TODO tx回调查单
            }
            PayOrder pov = pOrderMapper.getPOrderByOid(orderId);
            OrderQueryVO vo = new OrderQueryVO();
            vo.setStatus(pov.getOrderStatus());
//            vo.setPayUrl(pov.getResourceUrl());
            String envProxyUrl = vboxProxyMapper.getEnvUrl("loc");
            vo.setPayUrl(envProxyUrl + CommonConstant.ENV_HOST_PAY_URL + pov.getOrderId());
            vo.setCost(pov.getCost());
            vo.setOrderId(pov.getOrderId());
            vo.setNotifyUrl(pov.getNotifyUrl());

            return vo;
        }
    }

    @Override
    public Object handleRealOrder(HttpServletRequest request, String orderId) throws Exception {
        String ip = ProxyUtil.getIP(request);
        String region = CommonUtil.ip2region(ip);
        String userAgent = request.getHeader("User-Agent");
//        if (region == null || !region.contains("中国")) {
//            throw new ServiceException("该地区不允许操作");
//        }

        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
        if (poDB == null) throw new NotFoundException("该订单不存在， orderId :" + orderId);
        boolean has = redisUtil.hasKey(CommonConstant.ORDER_WAIT_QUEUE + orderId);
        if (has) {
            return 1;
        }
        if (poDB.getOrderStatus() != null && poDB.getOrderStatus() == 4) {
            log.info("handleRealOrder, 当前用户. ip check : {}", region);

            redisUtil.set(CommonConstant.ORDER_WAIT_QUEUE + orderId, 1);
            POrderQueue pOrderQueue = new POrderQueue();
            pOrderQueue.setPa(poDB.getPAccount());
            CChannel channel = channelMapper.getChannelByChannelId(poDB.getCChannelId());
            pOrderQueue.setChannel(channel.getId());
            pOrderQueue.setChannelId(poDB.getCChannelId());
            pOrderQueue.setOrderId(orderId);
            pOrderQueue.setPayIp(ip);
            pOrderQueue.setUserAgent(userAgent);
            pOrderQueue.setReqMoney(poDB.getCost());
//                pOrderQueue.setArea(area);
//            pOrderQueue.setAcid(poDB.getAcId());
//                pOrderQueue.setPr(pr);
            redisUtil.lPush(CommonConstant.ORDER_CREATE_QUEUE, pOrderQueue);

            pOrderMapper.updateRegionByOIdForSys(orderId, region);
//                createAsyncOrder()
            return 2;
        }


        return null;
    }

    @Override
    public Object tttt() throws IOException {
        getBalance("xoyokey=ZgzwL33ZL3eej0%3D%26%26xjarkI%3D%26%26a7Wko%3D%26%26r_U%3Dzg3zZggLLg%26WkawLL3e%26%26xkr%3DzZgLg%26%26%3DwlLZg_%3Deezcy%26oWgZee.WBm7gng.z%26raLk%3DZj3Zrco7.keBBz3z3cw3%26o; expires=Wed, 16-Aug-2023 19:08:19 GMT; path=/; domain=.xoyo.com; httponly", "wx12345678");
        return null;
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
                String cookie = "";
                boolean ckCheck = gee4Service.tokenCheck(c.getCk(), acAccount);
                if (ckCheck) {
                    cookie = c.getCk();
                    log.info("资源池计算. 库中ck取出...{}", c.getCk());
                } else {
                    cookie = getCKforQuery(acAccount, acPwd);
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

    @NotNull
    private List<CAccountInfo> computeAsync(Integer channelId, String now, List<CAccountInfo> cAccountList, List<CAccountInfo> cAccountListToday) throws IOException {
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
