package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.ResultOfList;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.PayTypeEnum;
import com.vbox.common.util.CommonUtil;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.exception.ServiceException;
import com.vbox.config.local.ProxyInfoThreadHolder;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.dto.ChannelAccountExcel;
import com.vbox.persistent.pojo.dto.TxWaterList;
import com.vbox.persistent.pojo.param.CAEnableParam;
import com.vbox.persistent.pojo.param.CAccountParam;
import com.vbox.persistent.pojo.param.ChannelBatchAcListParam;
import com.vbox.persistent.pojo.param.TxCAccountParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.ChannelService;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.SdoPayService;
import com.vbox.service.channel.TxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChannelServiceImpl implements ChannelService {

    @Autowired
    private CAccountMapper caMapper;
    @Autowired
    private CAccountDelMapper caDelMapper;
    @Autowired
    private CGatewayMapper cgMapper;
    @Autowired
    private RelationUSMapper relationUSMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VboxUserWalletMapper vboxUserWalletMapper;
    @Autowired
    private ChannelMapper channelMapper;
    @Autowired
    private PayService payService;
    @Autowired
    private TxPayService txPayService;
    @Autowired
    private SdoPayService sdoPayService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private POrderEventMapper pOrderEventMapper;
    @Autowired
    private POrderMapper pOrderMapper;
    @Autowired
    private ChannelPreMapper channelPreMapper;

    @Override
    public VboxUserVO getVboxUser() {

        Integer uid = TokenInfoThreadHolder.getToken().getId();
        User user = userMapper.selectById(uid);

        // 总账户充值
        Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

        // 总订单充值（花费）
        Integer totalCostSum = vboxUserWalletMapper.getTotalCostByUid(uid);

        // 总订单笔数（花费）
        Integer totalCostNum = vboxUserWalletMapper.getTotalCostNumByUid(uid);
        // 累计产生订单笔数
        Integer totalProdOrderNum = vboxUserWalletMapper.getTotalProdOrderNum(uid);

        totalRecharge = totalRecharge == null ? 0 : totalRecharge;
        totalCostSum = totalCostSum == null ? 0 : totalCostSum;
        // 总余额
        Integer balance = totalRecharge - totalCostSum;

        // 昨日订单数量
        Integer yesterdayOrderNum = vboxUserWalletMapper.getYesterdayOrderNum(uid);

        // 昨日订单数量
        Integer yesterdayProdOrderNum = vboxUserWalletMapper.getYesterdayProdOrderNum(uid);

        // 今日订单数量
        Integer todayOrderNum = vboxUserWalletMapper.getTodayOrderNum(uid);

        // 昨日订单金额
        Integer yesterdayOrderSum = vboxUserWalletMapper.getYesterdayOrderSum(uid);

        // 今日订单金额
        Integer todayOrderSum = vboxUserWalletMapper.getTodayOrderSum(uid);

        // 今日总产生订单量
        Integer todayProdOrderNum = vboxUserWalletMapper.getTodayProdOrderNum(uid);

        // 1hour成功充值金额
        Integer hourOrderSum = vboxUserWalletMapper.getHourOrderSum(uid);

        // 1hour成功订单量
        Integer hourOrderNum = vboxUserWalletMapper.getHourOrderNum(uid);

        // 1hour总产生订单量
        Integer hourProdOrderNum = vboxUserWalletMapper.getHourProdOrderNum(uid);


        VboxUserVO vo = new VboxUserVO();
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setBalance(balance);

        vo.setYesterdayOrderNum(yesterdayOrderNum == null ? 0 : yesterdayOrderNum);
        vo.setYesterdayProdOrderNum(yesterdayProdOrderNum == null ? 0 : yesterdayProdOrderNum);
        vo.setYesterdayOrderSum(yesterdayOrderSum == null ? 0 : yesterdayOrderSum);

        vo.setTodayOrderNum(todayOrderNum == null ? 0 : todayOrderNum);
        vo.setTodayOrderSum(todayOrderSum == null ? 0 : todayOrderSum);
        vo.setTodayProdOrderNum(todayProdOrderNum == null ? 0 : todayProdOrderNum);

        vo.setHourOrderSum(hourOrderSum == null ? 0 : hourOrderSum);
        vo.setHourOrderNum(hourOrderNum == null ? 0 : hourOrderNum);
        vo.setHourProdOrderNum(hourProdOrderNum == null ? 0 : hourProdOrderNum);

        vo.setTotalCostSum(totalCostSum);
        vo.setTotalCostNum(totalCostNum == null ? 0 : totalCostNum);
        vo.setTotalProdNum(totalProdOrderNum == null ? 0 : totalProdOrderNum);

        return vo;
    }

    @Override
    public List<Object> getVboxUserViewOrderSum() {

        List<Object> rsList = new ArrayList<>(36);
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(uid);

        sidList.add(uid);

        for (Integer sid : sidList) {
            List<CAccountWallet> sidTodayOrderList = vboxUserWalletMapper.getLast24HOrder(sid);
            Map<String, Object> sm = new HashMap<>();
            User user = userMapper.selectById(sid);
            sm.put("sid", sid);
            sm.put("account", user.getAccount());
            if (sidTodayOrderList == null || sidTodayOrderList.size() == 0) {
                sm.put("list", new HashMap<String, Integer>());
            } else {
                Map<String, Integer> sidCollect = sidTodayOrderList.stream().collect(
                        Collectors.groupingBy(v -> {
                                    LocalDateTime createTime = v.getCreateTime();
                                    String format = DateUtil.format(createTime, "dd-HH:mm");
//                                    int hour = createTime.getHour();
//                                    int minute = createTime.getMinute();
//                                    return hour + ":" + minute;
                                    return format;
                                }
                                , Collectors.summingInt(e -> {
                                    if (e.getCost() == null) {
                                        return 0;
                                    } else return e.getCost();
                                })
                        ));

                sm.put("list", new TreeMap<>(sidCollect));
            }
            rsList.add(sm);
        }

//        List<CAccountWallet> todayOrderList = vboxUserWalletMapper.getTodayOrder(uid);
//
//        if (todayOrderList == null || todayOrderList.size() == 0) {
//        }else {
//
//        }
//
//        Map<String, Integer> collect = todayOrderList.stream().collect(
//                Collectors.groupingBy(v -> {
//                            LocalDateTime createTime = v.getCreateTime();
//                            int hour = createTime.getHour();
//                            int minute = createTime.getMinute();
//                            return hour + ":" + minute;
//                        }
//                        , Collectors.summingInt(e -> {
//                            if (e.getCost() == null) {
//                                return 0;
//                            } else return e.getCost();
//                        })
//                ));

//        for (int i = 0; i < 24; i++) {
//            rsList.add(0);
//        }

//        collect.forEach(rsList::set);

        return rsList;
    }

    @Override
    public List<Long> getVboxUserViewOrderNum() {
        List<Long> rsList = new ArrayList<>(36);
        Integer uid = TokenInfoThreadHolder.getToken().getId();

        List<CAccountWallet> todayOrderList = vboxUserWalletMapper.getTodayOrder(uid);

        if (todayOrderList == null || todayOrderList.size() == 0) {
            return rsList;
        }

        Map<Integer, Long> collect = todayOrderList.stream().collect(
                Collectors.groupingBy(v -> {
                            LocalDateTime createTime = v.getCreateTime();
                            int hour = createTime.getHour();
                            return hour;
                        }
                        , Collectors.counting()
                ));

        for (int i = 0; i < 24; i++) {
            rsList.add(0L);
        }

        collect.forEach(rsList::set);

        return rsList;
    }

    @Override
    public int createChannelAccount(CAccountParam caParam) {

        CAccount ca = new CAccount();
        BeanUtils.copyProperties(caParam, ca);

        CGatewayInfo cgi = cgMapper.getGateWayInfoByCIdAndCG(caParam.getC_channel_id(), caParam.getC_gateway());
        ca.setCid(cgi.getCid());
        ca.setGid(cgi.getId());
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        String payDesc = PayTypeEnum.of(caParam.getPayType());
        ca.setAcAccount(caParam.getAc_account());
        ca.setAcPwd(Base64.encode(caParam.getAc_pwd()));
        ca.setAcRemark(caParam.getAc_remark());
        ca.setAcid(IdUtil.simpleUUID());
        ca.setUid(uid);
        ca.setCk(caParam.getCk());
        ca.setDailyLimit(caParam.getDaily_limit());
        ca.setTotalLimit(caParam.getTotal_limit());
        ca.setStatus(caParam.getStatus());
        ca.setPayDesc(payDesc);
        ca.setSysLog("初始化，暂未开启");
        ca.setSysStatus(0);

        caMapper.insert(ca);

        return 1;
    }

    @Override
    public int createSdoChannelAccount(CAccountParam caParam) {
        log.warn("createSdoChannelAccount, param: {}", caParam);
        String ck = caParam.getCk();
        String acPwd = Base64.encode(caParam.getAc_pwd());
//        String nsessionid = CommonUtil.getCookieValue(ck, "nsessionid");

//        boolean isValid = sdoPayService.tokenCheck(nsessionid);
//        if (!isValid) throw new ServiceException("ck传值错误，请核对");

        CAccount ca = new CAccount();
        BeanUtils.copyProperties(caParam, ca);

        CGatewayInfo cgi = cgMapper.getGateWayInfoByCIdAndCG(caParam.getC_channel_id(), caParam.getC_gateway());
        ca.setCid(cgi.getCid());
        ca.setGid(cgi.getId());
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        String payDesc = PayTypeEnum.of(caParam.getPayType());
        ca.setAcAccount(caParam.getAc_account());
        ca.setAcPwd(acPwd);
        ca.setAcRemark(caParam.getAc_remark());
        ca.setAcid(IdUtil.simpleUUID());
        ca.setUid(uid);
        ca.setCk(ck);
        ca.setDailyLimit(caParam.getDaily_limit());
        ca.setTotalLimit(caParam.getTotal_limit());
        ca.setStatus(caParam.getStatus());
        ca.setPayDesc(payDesc);
        ca.setSysLog("初始化，暂未开启");
        ca.setSysStatus(0);

        caMapper.insert(ca);
        return 0;
    }

    @Override
    public int createTxChannelAccount(TxCAccountParam caParam) {

        String ck = caParam.getCk();
        log.warn("param ck : {}", ck);
        String openID = CommonUtil.getCookieValue(ck, "openid");
        String openKey = CommonUtil.getCookieValue(ck, "openkey");
//
        boolean isValid = txPayService.tokenCheck(openID, openKey);
        if (!isValid) throw new ServiceException("openID、Key传值错误，请核对");

        CAccount ca = new CAccount();
        BeanUtils.copyProperties(caParam, ca);

        CGatewayInfo cgi = cgMapper.getGateWayInfoByCIdAndCG(caParam.getC_channel_id(), caParam.getC_gateway());
        ca.setCid(cgi.getCid());
        ca.setGid(cgi.getId());
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        String payDesc = PayTypeEnum.of(caParam.getPayType());
        ca.setAcAccount(caParam.getAc_account());
        // ck - pwd - openID
//        ca.setAcPwd(caParam.getOpenKey());
        ca.setAcPwd(openID);
        ca.setAcRemark(caParam.getAc_remark());
        ca.setAcid(IdUtil.simpleUUID());
        ca.setUid(uid);
        // ck - openKey
//        ca.setCk(ck);
        ca.setCk(openKey);
        ca.setDailyLimit(caParam.getDaily_limit());
        ca.setTotalLimit(caParam.getTotal_limit());
        ca.setStatus(caParam.getStatus());
        ca.setPayDesc(payDesc);
        ca.setSysLog("初始化，暂未开启");
        ca.setSysStatus(0);

        caMapper.insert(ca);
        return 0;
    }

    @Override
    public List<CGatewayVO> getGatewayList(String channelId) {

        List<CGatewayInfo> cgl = cgMapper.getGatewayListByCId(channelId);
        List<CGatewayVO> rs = new ArrayList<>();
        for (CGatewayInfo cg : cgl) {
            CGatewayVO v = CGatewayVO.transfer(cg);
            rs.add(v);
        }
        return rs;
    }

    @Override
    public ResultOfList<List<CAccountVO>> listCAccount(CAccountParam caParam) {

        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        Integer pageSize = caParam.getPageSize() == null ? 20 : caParam.getPageSize();
        Integer page = caParam.getPage() == null ? 0 : (caParam.getPage() - 1) * pageSize;

        String cChannelId = caParam.getC_channel_id();
        Integer cid = null;
        if (cChannelId != null) {
            CChannel chan = channelMapper.getChannelByChannelId(cChannelId);
            cid = chan.getId();
        }

        List<CAccount> caList = caMapper.listACInUids(sidList, caParam.getAc_remark(), cid, caParam.getStatus(), page, pageSize);
        Integer count = caMapper.countACInUids(sidList, caParam.getStatus(), page, pageSize);
        List<CAccountVO> acVOList = new ArrayList<>();
        for (CAccount ca : caList) {
            CAccountVO acv = CAccountVO.transfer(ca);
            CGatewayInfo gw = cgMapper.getGateWayInfoByCIdAndGId(ca.getCid(), ca.getGid());
            acv.setC_channel_id(gw.getCChannelId());
            acv.setC_channel_name(gw.getCGameName() + "-" + gw.getCChannelName());
            acv.setC_gateway_name(gw.getCGatewayName());
            acv.setAc_pwd(DesensitizedUtil.idCardNum(Base64.decodeStr(ca.getAcPwd()), 1, 1));
            int countPre = channelPreMapper.countForPreByACID(ca.getAcid());
            acv.setPre_count(countPre);
            //cost
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(ca.getId());
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(ca.getId());
            Integer yesterdayCost = vboxUserWalletMapper.getYesterdayOrderSumByCaid(ca.getId());
            acv.setToday_cost(todayCost == null ? 0 : todayCost);
            acv.setTotal_cost(totalCost == null ? 0 : totalCost);
            acv.setYesterday_cost(yesterdayCost == null ? 0 : yesterdayCost);
            acVOList.add(acv);
        }

        ResultOfList<List<CAccountVO>> rl = new ResultOfList<>(acVOList, count);

        return rl;
    }

    @Autowired
    private Gee4Service gee4Service;

    @Override
    public int updateCAccount(CAccountParam param) throws IOException {
        CAccount cAccount = new CAccount();

        String acPwd = param.getAc_pwd();
        String acAccount = param.getAc_account();
        if (param.getAc_pwd() != null) {
            String cookie = payService.getCKforQuery(acAccount, acPwd);

            boolean expire = gee4Service.tokenCheck(cookie, acAccount);

            if (!expire) {
                redisUtil.del(CommonConstant.ACCOUNT_CK + acAccount);
                cookie = payService.getCKforQuery(acAccount, Base64.decodeStr(acPwd));
                expire = gee4Service.tokenCheck(cookie, acAccount);
                if (!expire) {
//                    caMapper.stopByCaId("ck或密码有误，请更新", param.getId());
                    throw new NotFoundException("ck问题，请联系管理员");
                }
            }
            cAccount.setAcPwd(Base64.encode(param.getAc_pwd()));
        }

        cAccount.setId(param.getId());
        cAccount.setCk(param.getCk());

        cAccount.setTotalLimit(param.getTotal_limit());
        cAccount.setDailyLimit(param.getDaily_limit());
        cAccount.setMin(param.getMin());
        cAccount.setMax(param.getMax());
        cAccount.setAcRemark(param.getAc_remark());

        //判断用户余额是否足够
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        // 总账户充值
        Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

        // 总订单充值（花费）
        Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);

        totalRecharge = totalRecharge == null ? 0 : totalRecharge;
        totalCost = totalCost == null ? 0 : totalCost;
        // 总余额
        Integer balance = totalRecharge - totalCost;

        if (balance <= 0) {
            cAccount.setSysStatus(0);
            cAccount.setSysLog("总余额不足，请联系管理员充值");
        } else {
            cAccount.setSysStatus(1);
            cAccount.setSysLog("账户信息更新，系统判定可用");
        }
        return caMapper.updateById(cAccount);
    }

    @Override
    public int updateTxCAccount(TxCAccountParam param) {
        CAccount cAccount = new CAccount();

        String ck = param.getCk();
        String openID = CommonUtil.getCookieValue(ck, "openid");
        String openKey = CommonUtil.getCookieValue(ck, "openkey");

//        String openId = param.getOpenId();
//        String openKey = param.getOpenKey();
//        String acAccount = param.getAc_account();
        boolean isValid = txPayService.tokenCheck(openID, openKey);
        if (!isValid) throw new ServiceException("openID、Key传值错误，请核对");

        cAccount.setId(param.getId());
//        cAccount.setAcPwd(acAccount);
//        cAccount.setCk(ck);
        cAccount.setAcPwd(openID);
        cAccount.setCk(openKey);

        cAccount.setTotalLimit(param.getTotal_limit());
        cAccount.setDailyLimit(param.getDaily_limit());
        cAccount.setMin(param.getMin());
        cAccount.setMax(param.getMax());
//        cAccount.setAcRemark(param.getAc_remark());

        //判断用户余额是否足够
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        // 总账户充值
        Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

        // 总订单充值（花费）
        Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);

        totalRecharge = totalRecharge == null ? 0 : totalRecharge;
        totalCost = totalCost == null ? 0 : totalCost;
        // 总余额
        int balance = totalRecharge - totalCost;

        if (balance <= 0) {
            cAccount.setSysStatus(0);
            cAccount.setSysLog("总余额不足，请联系管理员充值");
        } else {
            cAccount.setSysStatus(1);
            cAccount.setSysLog("账户信息更新，系统判定可用");
        }
        return caMapper.updateById(cAccount);
    }

//    @Override
//    public int updateXoyCAccount(TxCAccountParam param) {
//        CAccount cAccount = new CAccount();
//
//        String ck = param.getCk();
//
//        String acAccount = param.getAc_account();
//
//        cAccount.setId(param.getId());
//        cAccount.setAcPwd(acAccount);
//        cAccount.setCk(ck);
//
//        cAccount.setTotalLimit(param.getTotal_limit());
//        cAccount.setDailyLimit(param.getDaily_limit());
//        cAccount.setMin(param.getMin());
//        cAccount.setMax(param.getMax());
//        cAccount.setAcRemark(param.getAc_remark());
//
//        //判断用户余额是否足够
//        Integer uid = TokenInfoThreadHolder.getToken().getId();
//        // 总账户充值
//        Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);
//
//        // 总订单充值（花费）
//        Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);
//
//        totalRecharge = totalRecharge == null ? 0 : totalRecharge;
//        totalCost = totalCost == null ? 0 : totalCost;
//        // 总余额
//        int balance = totalRecharge - totalCost;
//
//        if (balance <= 0) {
//            cAccount.setSysStatus(0);
//            cAccount.setSysLog("总余额不足，请联系管理员充值");
//        } else {
//            cAccount.setSysStatus(1);
//            cAccount.setSysLog("账户信息更新，系统判定可用");
//        }
//        return caMapper.updateById(cAccount);
//    }

    @Override
    public int updateSdoCAccount(CAccountParam param) throws IOException {
        CAccount cAccount = new CAccount();

        String ck = param.getCk();
        String acPwd = Base64.encode(param.getAc_pwd());
        ;
//        String nsessionid = CommonUtil.getCookieValue(ck, "nsessionid");

//        boolean isValid = sdoPayService.tokenCheck(ck);
//        if (!isValid) throw new ServiceException("ck传值错误，请核对");

        cAccount.setId(param.getId());
        cAccount.setAcPwd(acPwd);
        cAccount.setCk(ck);

        cAccount.setTotalLimit(param.getTotal_limit());
        cAccount.setDailyLimit(param.getDaily_limit());
        cAccount.setMin(param.getMin());
        cAccount.setMax(param.getMax());
        cAccount.setAcRemark(param.getAc_remark());

        //判断用户余额是否足够
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        // 总账户充值
        Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

        // 总订单充值（花费）
        Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);

        totalRecharge = totalRecharge == null ? 0 : totalRecharge;
        totalCost = totalCost == null ? 0 : totalCost;
        // 总余额
        int balance = totalRecharge - totalCost;

        if (balance <= 0) {
            cAccount.setSysStatus(0);
            cAccount.setSysLog("总余额不足，请联系管理员充值");
        } else {
            cAccount.setSysStatus(1);
            cAccount.setSysLog("账户信息更新，系统判定可用");
        }
        return caMapper.updateById(cAccount);
    }

    @Override
    public int updateXoyCAccount(CAccountParam param) throws IOException {
        CAccount cAccount = new CAccount();

        String ck = param.getCk();
        String acPwd = Base64.encode(param.getAc_pwd());

        cAccount.setId(param.getId());
        cAccount.setAcPwd(acPwd);
        cAccount.setCk(ck);

        cAccount.setTotalLimit(param.getTotal_limit());
        cAccount.setDailyLimit(param.getDaily_limit());
        cAccount.setMin(param.getMin());
        cAccount.setMax(param.getMax());
        cAccount.setAcRemark(param.getAc_remark());

        //判断用户余额是否足够
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        // 总账户充值
        Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

        // 总订单充值（花费）
        Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);

        totalRecharge = totalRecharge == null ? 0 : totalRecharge;
        totalCost = totalCost == null ? 0 : totalCost;
        // 总余额
        int balance = totalRecharge - totalCost;

        if (balance <= 0) {
            cAccount.setSysStatus(0);
            cAccount.setSysLog("总余额不足，请联系管理员充值");
        } else {
            cAccount.setSysStatus(1);
            cAccount.setSysLog("账户信息更新，系统判定可用");
        }

        return caMapper.updateById(cAccount);
    }

    @Override
    public int enableCAccount(CAEnableParam param) throws IOException {

        CAccount cAccount = new CAccount();
        cAccount.setId(param.getId());
        Integer status = param.getStatus();
        cAccount.setStatus(status);

        CAccount caDB = caMapper.selectById(param.getId());
        CChannel channel = channelMapper.getChannelById(caDB.getCid());
        Integer cid = channel.getId();

        if (cAccount.getStatus() == 1) {
            //jx 走这个逻辑
            if ("jx3".equals(channel.getCGame())) {
                log.warn("jx3 验证开启...");
                payService.addProxy(null, "127.0.0.1", null);

                String ck = payService.getCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                boolean expire = gee4Service.tokenCheck(ck, caDB.getAcAccount());

                if (!expire) {
                    redisUtil.del(CommonConstant.ACCOUNT_CK + caDB.getAcAccount());
                    ck = payService.getCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                    expire = gee4Service.tokenCheck(ck, caDB.getAcAccount());
                    if (!expire) {
//                        caMapper.stopByCaId("ck或密码有误，请更新", param.getId());
                        throw new NotFoundException("ck问题，请联系管理员");
                    }
                }
                log.warn("jx3 验证结果... {}", true);

                cAccount.setCk(ck);
            }
            //jx 走这个逻辑
            if ("sdo_in".equals(channel.getCGame())) {
                log.warn("sdo_in 验证开启...");
                Set<String> keys = redisUtil.getKeysByPattern(CommonConstant.CHANNEL_PROXY + "*");
                if (!keys.isEmpty()) {
                    String randomKey = keys.iterator().next();
                    redisUtil.del(randomKey);
                }
                payService.addProxy(null, "127.0.0.1", null);

                String ck = sdoPayService.getInnerCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                boolean expire = sdoPayService.tokenCheckInner(ck);

                if (!expire) {
                    redisUtil.del(CommonConstant.ACCOUNT_CK + caDB.getAcAccount());
                    ck = sdoPayService.getInnerCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                    expire = sdoPayService.tokenCheckInner(ck);
                    if (!expire) {
//                        caMapper.stopByCaId("ck或密码有误，请更新", param.getId());
                        throw new NotFoundException("ck问题，请联系管理员");
                    }
                }
                log.warn("sdo_in 验证结果... {}", true);

                cAccount.setCk(ck);
            }

//            if ("tx".equals(channel.getCGame())) {
//                //TODO
//                log.warn("tx系 验证开启...");
//                String openID = caDB.getAcPwd();
//                String openKey = caDB.getCk();
//                boolean isValid = txPayService.tokenCheck(openID, openKey);
//                log.warn("tx系 验证结果... {}", isValid);
//                if (!isValid) throw new ServiceException("openID、Key传值错误，请核对");
//            }

//            if ("sdo".equals(channel.getCGame())) {
//                log.warn("sdo系 验证开启...");
//                String nsessionid = caDB.getAcPwd();
//                boolean isValid = sdoPayService.tokenCheck(nsessionid);
//                if (!isValid) throw new ServiceException("ck传值错误或者过期，请核对");
//            }

            cAccount.setSysStatus(1);
            cAccount.setSysLog("账户手动设置开启");

            Integer dailyLimit = caDB.getDailyLimit();
            Integer totalLimit = caDB.getTotalLimit();
            Integer acID = caDB.getId();
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(acID);
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(acID);

            boolean flag = false;
            if (dailyLimit != null && dailyLimit > 0 && todayCost != null && todayCost >= dailyLimit) {
                cAccount.setSysStatus(0);
                cAccount.setStatus(0);
                cAccount.setSysLog("日内限额不足, 账户手动开启异常");
                log.info("手动开启但限额不足，当前 daily cost: {}, 当前账号: {}", todayCost, caDB);
            } else {
                //开启账号时候
                int row = channelPreMapper.startPreLinkWhenStartAC(caDB.getAcid(), channel.getCChannelId());
                log.warn("开启账号时，预产记录置为可用, info : {}, ca : {}", row, caDB);
                flag = true;
            }

            if (totalLimit != null && totalLimit > 0 && totalCost != null && totalCost >= totalLimit) {
                cAccount.setSysStatus(0);
                cAccount.setStatus(0);
                cAccount.setSysLog("总限额不足, 账户手动开启异常");
                log.info("手动开启但限额不足，当前 total cost: {}, 当前账号: {}", totalCost, caDB);
            } else {
                if (!flag) {
                    //开启账号时候
                    int row = channelPreMapper.startPreLinkWhenStartAC(caDB.getAcid(), channel.getCChannelId());
                    log.warn("开启账号时，预产记录置为可用, info : {}, ca : {}", row, caDB);
                }
            }

        } else {
            //关闭账号时候
            int row = channelPreMapper.stopPreLinkWhenStartAC(caDB.getAcid(), channel.getCChannelId());
            log.warn("关闭账号时，预产记录置为不可用, info : {}, ca : {}", row, caDB);
            Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":*");
            for (String key : keys) {
                log.warn("关闭账号时，清理通道keys: {}", key);
                redisUtil.del(key);
            }
        }

        return caMapper.updateById(cAccount);
    }

    @Override
    public int enableBatchCAccount(List<String> acidList, Integer status) throws IOException {

        log.warn("批量执行开关账号，当前执行动作: status - [{}]", status);

        int successCount = 0;
        int errCount = 0;
        for (String acid : acidList) {

            CAccount caDB = caMapper.getCAccountByAcid(acid);
            try {
                caDB.setStatus(status);

                CChannel channel = channelMapper.getChannelById(caDB.getCid());
                Integer cid = channel.getId();

                if (caDB.getStatus() == 1) {
                    //jx 走这个逻辑
                    if ("jx3".equals(channel.getCGame())) {
                        log.warn("jx3 验证开启...");
                        payService.addProxy(null, "127.0.0.1", null);

                        String ck = payService.getCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                        boolean expire = gee4Service.tokenCheck(ck, caDB.getAcAccount());

                        if (!expire) {
                            redisUtil.del(CommonConstant.ACCOUNT_CK + caDB.getAcAccount());
                            ck = payService.getCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                            expire = gee4Service.tokenCheck(ck, caDB.getAcAccount());
                            if (!expire) {
//                                caMapper.stopByCaId("ck或密码有误，请更新", caDB.getId());
                                throw new NotFoundException("ck问题，请联系管理员");
                            }
                        }
                        log.warn("jx3 验证结果... {}", true);

                        caDB.setCk(ck);
                    }

                    caDB.setSysStatus(1);
                    caDB.setSysLog("账户手动设置开启");

                    Integer dailyLimit = caDB.getDailyLimit();
                    Integer totalLimit = caDB.getTotalLimit();
                    Integer acID = caDB.getId();
                    Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(acID);
                    Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(acID);

                    boolean flag = false;
                    if (dailyLimit != null && dailyLimit > 0 && todayCost != null && todayCost >= dailyLimit) {
                        caDB.setSysStatus(0);
                        caDB.setStatus(0);
                        caDB.setSysLog("日内限额不足, 账户手动开启异常");
                        log.info("手动开启但限额不足，当前 daily cost: {}, 当前账号: {}", todayCost, caDB);
                    } else {
                        //开启账号时候
                        int row = channelPreMapper.startPreLinkWhenStartAC(caDB.getAcid(), channel.getCChannelId());
                        log.warn("开启账号时，预产记录置为可用, info : {}, ca : {}", row, caDB);
                        flag = true;
                    }

                    if (totalLimit != null && totalLimit > 0 && totalCost != null && totalCost >= totalLimit) {
                        caDB.setSysStatus(0);
                        caDB.setStatus(0);
                        caDB.setSysLog("总限额不足, 账户手动开启异常");
                        log.info("手动开启但限额不足，当前 total cost: {}, 当前账号: {}", totalCost, caDB);
                    } else {
                        if (!flag) {
                            //开启账号时候
                            int row = channelPreMapper.startPreLinkWhenStartAC(caDB.getAcid(), channel.getCChannelId());
                            log.warn("开启账号时，预产记录置为可用, info : {}, ca : {}", row, caDB);
                        }
                    }

                } else {
                    //关闭账号时候
                    int row = channelPreMapper.stopPreLinkWhenStartAC(caDB.getAcid(), channel.getCChannelId());
                    log.warn("关闭账号时，预产记录置为不可用, info : {}, ca : {}", row, caDB);
                    Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + cid + ":*");
                    for (String key : keys) {
                        log.warn("关闭账号时，清理通道keys: {}", key);
                        redisUtil.del(key);
                    }
                }
                int row = caMapper.updateById(caDB);
                successCount++;
                log.warn("caDB enable row: [{}], acc : {}", row, caDB.getAcAccount());
            } catch (Exception e) {
                errCount++;
                log.error("第 {} 行记录参数异常，跳过， info ： {}", errCount, e);
            }
        }
        log.warn("共计本次批量开关总计： {} 条, : {} 条成功, : {} 条失败", acidList.size(), successCount, errCount);
        return 0;
    }

    @Override
    public int deleteCAccount(Integer id) {

        CAccount ca = caMapper.selectById(id);

        String acid = ca.getAcid();
        Integer count = pOrderMapper.countPOrderByAcId(acid);
        if (count == 0) {
            Integer chanId = ca.getCid();
            CChannel channel = channelMapper.getChannelById(chanId);
            if (channel.getCChannelId().contains("sdo_alipay") || channel.getCChannelId().contains("jx3_alipay_pre")) {
                //删预产
                int row = channelPreMapper.deleteByACID(acid);
                log.warn("删除账号时，删预产记录, info : {}, ca : {}", row, ca);

                Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + chanId + ":*");
                for (String key : keys) {
                    log.warn("删除账号时，清理通道keys: {}", key);
                    redisUtil.del(key);
                }
            }
            return caMapper.deleteById(id);
        } else { //有过订单，软删
            Integer countToday = pOrderMapper.countPOrderByAcIdToday(acid);
            if (countToday == 0) {
                CAccountDel upd = new CAccountDel();
                BeanUtils.copyProperties(ca, upd);
                upd.setId(null);
                caMapper.deleteById(id);

                Integer chanId = ca.getCid();
                CChannel channel = channelMapper.getChannelById(chanId);
                if (channel.getCChannelId().contains("sdo_alipay") || channel.getCChannelId().contains("jx3_alipay_pre")) {
                    //删预产
                    int row = channelPreMapper.deleteByACID(acid);
                    log.warn("删除账号时，删预产记录, info : {}, ca : {}", row, ca);

                    Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + chanId + ":*");
                    for (String key : keys) {
                        log.warn("删除账号时，清理通道keys: {}", key);
                        redisUtil.del(key);
                    }
                }
                return caDelMapper.insert(upd);
            } else {
                throw new ServiceException("该账号不允许删除");
            }

        }
    }

    @Autowired
    private VboxProxyMapper vboxProxyMapper;
    //
    @Override
    public Object getTxQuery(String orderId) {
        PayOrder po = pOrderMapper.getPOrderByOid(orderId);
        String formUrl = "";
        if (po.getCChannelId().contains("jx3")) {
            formUrl = "https://charge.xoyo.com/charge-record";
            if (po.getCChannelId().contains("jx3_alipay_pre")) {
                formUrl = channelPreMapper.getAddressByPlatOid(po.getPlatformOid());
            }
        } else if (po.getCChannelId().contains("sdo")) {
            formUrl = channelPreMapper.getAddressByPlatOid(po.getPlatformOid());
            if (po.getCChannelId().contains("sdo_in")) {
                payService.addProxy(null, "127.0.0.1", null);

                CAccount ca = caMapper.getCAccountByAcid(po.getAcId());

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
                    if (paidCount == 1) {
                        qryRes.put("msg", "已支付");
                        return qryRes;
                    }else {
                        qryRes.put("msg", "未支付");
                        return qryRes;
                    }
                }
            }
        } else if (po.getCChannelId().contains("wme")) {
            PayOrderEvent event = pOrderEventMapper.getPOrderEventByOid(po.getOrderId());
            formUrl = event.getExt();
        } else if (po.getCChannelId().contains("xoy")) {
            String acId = po.getAcId();
            CAccount caDB = caMapper.getCAccountByAcid(acId);
            CGatewayInfo cgi = cgMapper.getGateWayInfoByCIdAndGId(caDB.getCid(), caDB.getGid());

            JSONObject data = payService.getBalanceJson2JXAcc(cgi.getCGateway(), caDB);
            return data;
        } else {
            CAccount ca = caMapper.getCAccountByAcid(po.getAcId());
            String openID = ca.getAcPwd();
            String openKey = ca.getCk();
//            formUrl = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=openid&sessiontype=kp_accesstoken&openid=5941CB1704D389951E4F7A700792CFF5&openkey=28C5A26217AF878E2C155BF2CCFF9977#/"
            formUrl = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=openid&sessiontype=kp_accesstoken&openid=" + openID + "&openkey=" + openKey + "#/";
//            String qq = ca.getAcAccount();
//            List<TxWaterList> txWaterListList = txPayService.queryOrderAll(openID, openKey);

//            formUrl = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=hy_gameid&sessiontype=st_dummy&openkey=openkey&openid=" + qq + "#/";
//            return txWaterListList;
        }

        return formUrl;
    }

    @Override
    public Object getAccQuery(String acid) {
        CAccount caDB = caMapper.getCAccountByAcid(acid);
        String formUrl = "";
        CGatewayInfo cgi = cgMapper.getGateWayInfoByCIdAndGId(caDB.getCid(), caDB.getGid());

        if (cgi.getCChannelId().contains("jx3")) {
            formUrl = "https://charge.xoyo.com/charge-record";
//            if (cgi.getCChannelId().contains("jx3_alipay_pre")) {
//                formUrl = channelPreMapper.getAddressByPlatOid(po.getPlatformOid());
//            }
//        } else if (cgi.getCChannelId().contains("sdo")) {
//            formUrl = channelPreMapper.getAddressByPlatOid(po.getPlatformOid());
//        } else if (cgi.getCChannelId().contains("wme")) {
//            PayOrderEvent event = pOrderEventMapper.getPOrderEventByOid(po.getOrderId());
//            formUrl = event.getExt();
        } else if (cgi.getCChannelId().contains("xoy")) {
            JSONObject data = payService.getBalanceJson2JXAcc(cgi.getCGateway(), caDB);
            return data;
        } else {
//            String qq = caDB.getAcAccount();
//            formUrl = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=hy_gameid&sessiontype=st_dummy&openkey=openkey&openid=" + qq + "#/";
            String openID = caDB.getAcPwd();
            String openKey = caDB.getCk();
            formUrl = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=openid&sessiontype=kp_accesstoken&openid=" + openID + "&openkey=" + openKey + "#/";
//            String qq = caDB.getAcAccount();
//            List<TxWaterList> txWaterListList = txPayService.queryOrderAll(openID, openKey);

//            formUrl = "https://pay.qq.com/h5/trade-record/trade-record.php?appid=1450000186&_wv=1024&pf=2199&sessionid=hy_gameid&sessiontype=st_dummy&openkey=openkey&openid=" + qq + "#/";
//            return txWaterListList;
        }

        return formUrl;
    }

    @Override
    public int batchChannelAccount(MultipartFile multipartFile) {

        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<ChannelAccountExcel> preExcelList;
        try {
            preExcelList = CommonUtil.parseChannelAccountExcel(multipartFile);
//            if (preExcelList.size() > 50) {
//                log.error("batchChannelPre. 超出上限，一次最多50个");
//                throw new ServiceException("文件解析异常");
//            }
        } catch (IOException e) {
            log.error("batchChannelAccount. 上传文件解析异常");
            throw new ServiceException("文件解析异常");
        }
        log.warn("本次批量导入 start ... uid: {}", uid);

        int count = 0;
        int errCount = 0;

        for (ChannelAccountExcel caExcel : preExcelList) {
            try {
                CAccountParam caParam = new CAccountParam();

                CAccount ca = new CAccount();
                BeanUtils.copyProperties(caExcel, caParam);
                log.warn("本次 caExcel ... {}", caExcel);

                CGatewayInfo cgi = cgMapper.getGateWayInfoByCIdAndCG(caParam.getC_channel_id(), caParam.getC_gateway());
                ca.setCid(cgi.getCid());
                ca.setGid(cgi.getId());
                String payDesc = PayTypeEnum.of(caParam.getPayType());
                ca.setAcAccount(caParam.getAc_account());
                ca.setAcPwd(Base64.encode(caParam.getAc_pwd()));
                ca.setAcRemark(caParam.getAc_remark());
                ca.setAcid(IdUtil.simpleUUID());
                ca.setUid(uid);
                ca.setCk(caParam.getCk());
                ca.setDailyLimit(caParam.getDaily_limit());
                ca.setTotalLimit(caParam.getTotal_limit());
                ca.setStatus(caParam.getStatus());
                ca.setPayType(caParam.getPayType());
                ca.setPayDesc(payDesc);
                ca.setSysLog("初始化，暂未开启");
                ca.setSysStatus(0);

                caMapper.insert(ca);

                count++;
            } catch (BeansException e) {
                errCount++;
                log.error("第 {} 行记录参数异常，跳过， info ： {}", count, caExcel, e);
            }
        }

        log.warn("batchChannelAccount.共计本次批量导入总计： {} 条, : {} 条成功, : {} 条失败", preExcelList.size(), count, errCount);

        return 1;
    }

    @Override
    public int deleteBatchCAccount(List<String> acidList) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        log.warn("本次批量删除 start ... uid: {}", uid);

        int successCount = 0;
        int errCount = 0;

        Set<Integer> chanIdList = new HashSet<>();

        for (String acid : acidList) {
            try {
                CAccount ca = caMapper.getCAccountByAcid(acid);
                Integer id = ca.getId();
                Integer count = pOrderMapper.countPOrderByAcId(acid);
                Integer chanId = ca.getCid();
                CChannel channel = channelMapper.getChannelById(chanId);
                chanIdList.add(chanId);
                if (count == 0) {
                    if (channel.getCChannelId().contains("sdo_alipay") || channel.getCChannelId().contains("jx3_alipay_pre")) {
                        //删预产
                        int row = channelPreMapper.deleteByACID(acid);
                        log.warn("批量删除账号时，删预产记录, info : {}, ca : {}", row, ca);
                    }
                    //                Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + chanId + ":*");
                    //                for (String key : keys) {
                    //                    log.warn("删除账号时，清理通道keys: {}", key);
                    //                    redisUtil.del(key);
                    //                }
                    int row = caMapper.deleteById(id);
                    log.warn("无订单，直接删，row: {},  info {}", row, ca);
                } else { //有过订单，软删
                    CAccountDel upd = new CAccountDel();
                    BeanUtils.copyProperties(ca, upd);
                    upd.setId(null);
                    int rowD = caMapper.deleteById(id);

                    if (channel.getCChannelId().contains("sdo_alipay") || channel.getCChannelId().contains("jx3_alipay_pre")) {
                        //删预产
                        int row = channelPreMapper.deleteByACID(acid);
                        log.warn("批量删除账号时，删预产记录, info : {}, ca : {}", row, ca);
                    }
                    //                Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + chanId + ":*");
                    //                for (String key : keys) {
                    //                    log.warn("删除账号时，清理通道keys: {}", key);
                    //                    redisUtil.del(key);
                    //                }
                    int rowI = caDelMapper.insert(upd);

                    log.warn("有订单，软删，row: {},  info {}", rowD + rowI, ca);
                }
                successCount++;
            } catch (BeansException e) {
                errCount++;
                log.error("第 {} 行记录参数异常，跳过， info ： {}", errCount, e);
            }
        }

        for (Integer chanId : chanIdList) {
            Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + chanId + ":*");
            for (String key : keys) {
                log.warn("批量删除账号时，清理通道keys: {}", key);
                redisUtil.del(key);
            }
        }

        log.warn("batchChannelAccount.共计本次批量删除总计： {} 条, : {} 条成功, : {} 条失败", acidList.size(), successCount, errCount);

        return 0;
    }

    @Override
    public List<CAccount> listAllCAccount(CAccountParam param) {
        Integer currentUid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = relationUSMapper.listSidByUid(currentUid);

        sidList.add(currentUid);

        Integer cid = null;
        Integer status = param.getStatus();

        String channel = param.getC_channel_id();
        if (channel != null) {
            CChannel chan = channelMapper.getChannelByChannelId(channel);
            cid = chan.getId();
        }

        List<CAccount> caList = caMapper.listACInUids(sidList, null, cid, status, 0, 999);

        return caList;
    }

    //废弃
    @Override
    public int enableCAccountList(ChannelBatchAcListParam param) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        log.warn("本次批量删除 start ... uid: {}", uid);

        List<String> acidList = param.getAcidList();
        int successCount = 0;
        int errCount = 0;

        Set<Integer> chanIdList = new HashSet<>();

        for (String acid : acidList) {
            CAccount ca = caMapper.getCAccountByAcid(acid);
            Integer id = ca.getId();
        }
        return 0;
    }
}
