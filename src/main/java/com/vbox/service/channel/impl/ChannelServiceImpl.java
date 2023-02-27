package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.IdUtil;
import com.vbox.common.ResultOfList;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.enums.PayTypeEnum;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.exception.NotFoundException;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.CAccountWallet;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.pojo.param.CAEnableParam;
import com.vbox.persistent.pojo.param.CAccountParam;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.CGatewayVO;
import com.vbox.persistent.pojo.vo.VboxUserVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.ChannelService;
import com.vbox.service.channel.PayService;
import com.vbox.service.task.Gee4Service;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChannelServiceImpl implements ChannelService {

    @Autowired
    private CAccountMapper caMapper;
    @Autowired
    private CGatewayMapper cgMapper;
    @Autowired
    private RelationUSMapper relationUSMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VboxUserWalletMapper vboxUserWalletMapper;
    @Autowired
    private PayService payService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private POrderMapper pOrderMapper;

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
    public List<Integer> getVboxUserViewOrderSum() {

        List<Integer> rsList = new ArrayList<>(36);
        Integer uid = TokenInfoThreadHolder.getToken().getId();

        List<CAccountWallet> todayOrderList = vboxUserWalletMapper.getTodayOrder(uid);

        if (todayOrderList == null || todayOrderList.size() == 0) {
            return rsList;
        }

        Map<Integer, Integer> collect = todayOrderList.stream().collect(
                Collectors.groupingBy(v -> {
                            LocalDateTime createTime = v.getCreateTime();
                            int hour = createTime.getHour();
                            return hour;
                        }
                        , Collectors.summingInt(e -> {
                            if (e.getCost() == null) {
                                return 0;
                            } else return e.getCost();
                        })
                ));

        for (int i = 0; i < 24; i++) {
            rsList.add(0);
        }

        collect.forEach(rsList::set);

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
        ca.setDailyLimit(caParam.getDaily_limit());
        ca.setTotalLimit(caParam.getTotal_limit());
        ca.setStatus(caParam.getStatus());
        ca.setPayDesc(payDesc);
        ca.setSysLog("初始化，暂未开启");
        ca.setSysStatus(2);

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

        List<CAccount> caList = caMapper.listACInUids(sidList);
        List<CAccountVO> acVOList = new ArrayList<>();
        for (CAccount ca : caList) {
            CAccountVO acv = CAccountVO.transfer(ca);
            CGatewayInfo gw = cgMapper.getGateWayInfoByCIdAndGId(ca.getCid(), ca.getGid());
            acv.setC_channel_id(gw.getCChannel());
            acv.setC_channel_name(gw.getCGameName() + "-" + gw.getCChannelName());
            acv.setC_gateway_name(gw.getCGatewayName());
            acv.setAc_pwd(DesensitizedUtil.idCardNum(Base64.decodeStr(ca.getAcPwd()), 1, 1));

            //cost
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(ca.getId());
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(ca.getId());
            acv.setToday_cost(todayCost == null ? 0 : todayCost);
            acv.setTotal_cost(totalCost == null ? 0 : totalCost);

            acVOList.add(acv);
        }

        ResultOfList<List<CAccountVO>> rl = new ResultOfList<>(acVOList, acVOList.size());

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
            String cookie = payService.getCK(acAccount, acPwd);

            boolean expire = gee4Service.tokenCheck(cookie, acAccount);

            if (!expire) {
                redisUtil.del(CommonConstant.ACCOUNT_CK + acAccount);
                cookie = payService.getCK(acAccount, Base64.decodeStr(acPwd));
                expire = gee4Service.tokenCheck(cookie, acAccount);
                if (!expire) {
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
    public int enableCAccount(CAEnableParam param) throws IOException {

        CAccount cAccount = new CAccount();
        cAccount.setId(param.getId());
        Integer status = param.getStatus();
        cAccount.setStatus(status);

        CAccount caDB = caMapper.selectById(param.getId());

        if (cAccount.getStatus() == 1) {
            String ck = payService.getCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
            boolean expire = gee4Service.tokenCheck(ck, caDB.getAcAccount());

            if (!expire) {
                redisUtil.del(CommonConstant.ACCOUNT_CK + caDB.getAcAccount());
                ck = payService.getCK(caDB.getAcAccount(), Base64.decodeStr(caDB.getAcPwd()));
                expire = gee4Service.tokenCheck(ck, caDB.getAcAccount());
                if (!expire) {
                    throw new NotFoundException("ck问题，请联系管理员");
                }
            }

            cAccount.setCk(ck);
            cAccount.setSysStatus(1);
            cAccount.setSysLog("账户手动设置开启");
        }

        return caMapper.updateById(cAccount);
    }

    @Override
    public int deleteCAccount(Integer cid) {

        CAccount ca = caMapper.selectById(cid);

        Integer count = pOrderMapper.countPOrderByAcId(ca.getAcid());
        if (count == 0) {
            return caMapper.deleteById(cid);
        }else { //有过订单，软删
            CAccount upd = new CAccount();
            upd.setId(cid);
            upd.setSoftDel(0);
            return caMapper.updateById(upd);
        }
    }
}
