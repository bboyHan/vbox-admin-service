package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

        // 1. 校验sign
//        PayerInfo payerLocal = PayerInfoThreadHolder.getPayerInfo();
        String pa = orderCreateParam.getP_account();
        String sign = orderCreateParam.getSign();
        PAccount paDB = pAccountMapper.selectOne(new QueryWrapper<PAccount>().eq("p_account", pa));
        String pKey = paDB.getPKey();
        orderCreateParam.setSign(null);
        SortedMap<String, String> map = CommonUtil.objToTreeMap(orderCreateParam);

        String signDB = CommonUtil.encodeSign(map, pKey);
        if (!signDB.equals(sign)) throw new ValidateException("入参仅限文档包含字段，请核对");
//        boolean valid = PayerInfo.valid(payerLocal, new PayerInfo(pub, pa));
//        if (!valid) throw new ValidateException("Token valid");

        // 2. param check
        String channelId = orderCreateParam.getChannel_id();
        Channel channel = channelMapper.getChannelByChannelId(channelId);
        if (channel == null) throw new ValidateException("通道id错误，请重新查询确认");
        String notify = orderCreateParam.getNotify_url();
        boolean isUrl = CommonUtil.isUrl(notify);
        if (!isUrl) throw new ValidateException("notify_url不合法，请检验入参");

        String attach = orderCreateParam.getAttach();
        if (attach != null && attach.length() > 128) throw new ValidateException("attach不合法，请检验入参");
        String orderId = orderCreateParam.getP_order_id();
        if (orderId == null || orderId.length() > 32 || orderId.length() < 16)
            throw new ValidateException("orderId不合法，请检验入参");
        PayOrder poDB = pOrderMapper.getPOrderByOid(orderId);
        if (poDB != null) throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);

        List<CAccountInfo> cAccountList = cAccountMapper.listCanPayForCAccount();
        if (cAccountList == null || cAccountList.size() == 0) {
            throw new NotFoundException("系统不可用充值渠道，请联系管理员");
        }

        String orderKey = "redisLock_order:" + orderId;
        if (redisUtil.hasKey(orderKey)) {
            throw new DuplicateKeyException("该订单已创建，请勿重复操作，order id: " + orderId);
        }
        redisUtil.set(orderKey, 1, 300);
        log.info("create order 创建订单: {}, p account: {}", orderId, pa);

        // 3. 根据付方所需金额，列出可充值资源池
        Integer reqMoney = orderCreateParam.getMoney();

        LocalDateTime nowTime = LocalDateTime.now();
        String now = DateUtil.format(nowTime, "yyyy-MM-dd");
        // 拿到所有 channel account
        List<CAccountInfo> cAccountListToday = cAccountMapper.listCanPayForCAccountToday(now);
        for (CAccountInfo c : cAccountListToday) {
            c.setCreateTime(nowTime);
        }

        // 计算可用
        List<CAccountInfo> randomTemp = compute(channel.getId(), now, cAccountList, cAccountListToday);

        if (randomTemp.size() == 0) {
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
            throw new ServiceException("订单创建失败，请联系管理员");
        }
        if (orderResp.getInteger("code") != 1) {
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
        String h5Url = "http://mng.vboxjjjxxx.info/#/code/pay?payUrl=" + Base64.encode(resource_url);

        payOrder.setResourceUrl(h5Url);
        payOrder.setNotifyUrl(notify);
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
        boolean delaySetting = redisUtil.zAdd(CommonConstant.ORDER_DELAY_QUEUE, delayTask, 300000); //1000ms
        if (delaySetting) {
            log.info("delay info: {}, expire time: {}", delayTask, 20000);
        }

        PayOrderCreateVO p = new PayOrderCreateVO();
        p.setPayUrl(h5Url);
        p.setOrderId(orderId);
        p.setCost(reqMoney);
        p.setAttach(attach);
        p.setStatus(2);

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
            log.warn("[notify] call back to me pay failed, order info: {}", po);
            pOrderMapper.updateStatusByOIdWhenCall(orderId, payStatus, CodeStatusEnum.FAILED.getCode());
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

        OrderQueryVO vo = new OrderQueryVO();
        vo.setStatus(po.getOrderStatus());
        vo.setPayUrl(po.getResourceUrl());
        vo.setCost(po.getCost());
        vo.setOrderId(po.getOrderId());
        vo.setNotifyUrl(po.getNotifyUrl());
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
        ClassPathResource classPathResource = new ClassPathResource("d4.js");
        InputStream is = classPathResource.getInputStream();
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
                JSONObject data = obj.getJSONObject("data");
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
        payNotifyVO.setStatus(payOrder.getOrderStatus());
        payNotifyVO.setP_account(pa);
        String sign = CommonUtil.encodeSign(CommonUtil.objToTreeMap(payNotifyVO), pKey);
        payNotifyVO.setSign(sign);

        String body = HttpRequest.post(payOrder.getNotifyUrl())
                .body(JSONObject.toJSONString(payNotifyVO))
                .execute().body();

        pOrderMapper.updateCallbackStatusByOId(orderId);

        log.info("测试回调商户，商户返回信息： {} ", body);
        return body;
    }

    @Override
    public PayOrderCreateVO orderQuery(String payStr) {
        PayOrder payOrder = pOrderMapper.selectOne(new QueryWrapper<PayOrder>().eq("resource_url",
                "http://mng.vboxjjjxxx.info/#/code/pay?payUrl=" +payStr));
        if (payOrder == null) throw new NotFoundException("订单不存在，请联系商家核对");
        PayOrderCreateVO payOrderCreateVO = new PayOrderCreateVO();
        payOrderCreateVO.setPayUrl(payOrder.getResourceUrl());
        payOrderCreateVO.setCost(payOrder.getCost());
        payOrderCreateVO.setStatus(payOrder.getOrderStatus());
        payOrderCreateVO.setOrderId(payOrder.getOrderId());
        return payOrderCreateVO;
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
