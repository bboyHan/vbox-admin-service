package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base32;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.ResultOfList;
import com.vbox.common.enums.LoginEnum;
import com.vbox.common.util.RandomNameUtil;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.pojo.vo.SaleVO;
import com.vbox.persistent.pojo.vo.TotalVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.SaleService;
import com.vbox.service.system.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SaleServiceImpl implements SaleService {

    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserExtMapper userExtMapper;
    @Autowired
    private UserLoginMapper userLoginMapper;
    @Autowired
    private UserAuthMapper userAuthMapper;
    @Autowired
    private CAccountMapper cAccountMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private RelationUDMapper udMapper;
    @Autowired
    private RelationURMapper urMapper;
    @Autowired
    private RelationUSMapper usMapper;
    @Autowired
    private CGatewayMapper cGatewayMapper;
    @Autowired
    private VboxUserWalletMapper vboxUserWalletMapper;

    @Override
    public TotalVO totalOverView() {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = this.usMapper.listSidByUid(uid);
        if (sidList != null && sidList.size() != 0) {
            Integer totalRecharge = this.vboxUserWalletMapper.getTotalRechargeBySidList(sidList);
            Integer totalCost = this.vboxUserWalletMapper.getTotalCostBySidList(sidList);
            Integer totalCostNum = this.vboxUserWalletMapper.getTotalCostNumBySidList(sidList);
            Integer totalNum = this.vboxUserWalletMapper.getTotalNumBySidList(sidList);
            TotalVO totalVO = new TotalVO();
            totalVO.setRecharge(totalRecharge);
            totalVO.setTotalCost(totalCost);
            totalVO.setBalance(totalRecharge - totalCost);
            totalVO.setTotalNum(totalNum);
            totalVO.setTotalCostNum(totalCostNum);
            return totalVO;
        } else {
            return null;
        }
    }

    @Autowired
    private POrderMapper pOrderMapper;

    @Override
    public ResultOfList listSaleCAOverviewToday() {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = this.usMapper.listSidByUid(uid);

        for (Integer sid : sidList) {
            List<CAccount> acList = cAccountMapper.listAcInUid(sid);
            Set<String> acidSet = cAccountMapper.setAcIdInUid(sid);
            List<PayOrder> poList = pOrderMapper.selectList(new QueryWrapper<PayOrder>()
                    .in("ac_id", acidSet)
            );
            if (poList != null && poList.size() > 0) {
                Map<String, List<PayOrder>> collect = poList.stream().collect(Collectors.groupingBy(PayOrder::getAcId));
            }
        }
        return null; //TODO
    }

    @Override
    public List<SaleVO> listSaleOverView() {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = this.usMapper.listSidByUid(uid);
        if (sidList != null && sidList.size() != 0) {
            List<User> userList = this.userMapper.selectBatchIds(sidList);
            List<SaleVO> saleVOList = new ArrayList<>();

            for (User user : userList) {
                Integer sid = user.getId();
                Integer totalCost = this.vboxUserWalletMapper.getTotalCostByUid(sid);
                Integer totalCostNum = this.vboxUserWalletMapper.getTotalCostNumByUid(sid);
                Integer totalProdOrderNum = this.vboxUserWalletMapper.getTotalProdOrderNum(sid);

                Integer todayProdOrderNum = this.vboxUserWalletMapper.getTodayProdOrderNum(sid);
                Integer todayOrderNum = this.vboxUserWalletMapper.getTodayOrderNum(sid);
                Integer todayOrderSum = this.vboxUserWalletMapper.getTodayOrderSum(sid);

                Integer yesterdayProdNum = this.vboxUserWalletMapper.getYesterdayProdOrderNum(sid);
                Integer yesterdayNum = this.vboxUserWalletMapper.getYesterdayOrderNum(sid);
                Integer yesterdayOrderSum = this.vboxUserWalletMapper.getYesterdayOrderSum(sid);

                Integer countCA = this.cAccountMapper.countByUid(sid);
                Integer countEnableCA = this.cAccountMapper.countACEnableByUid(sid);
                Integer totalRecharge = this.vboxUserWalletMapper.getTotalRechargeByUid(sid);
                SaleVO saleVO = new SaleVO();
                saleVO.setId(sid);
                saleVO.setAccount(user.getAccount());
                saleVO.setNickname(user.getNickname());
                totalCost = totalCost == null ? 0 : totalCost;

                saleVO.setTotalCost(totalCost);
                saleVO.setTotalCostNum(totalCostNum == null ? 0 : totalCostNum);
                saleVO.setTotalNum(totalProdOrderNum == null ? 0 : totalProdOrderNum);

                saleVO.setTodayProdOrderNum(todayProdOrderNum == null ? 0 : todayProdOrderNum);
                saleVO.setTodayOrderNum(todayOrderNum == null ? 0 : todayOrderNum);
                saleVO.setTodayOrderSum(todayOrderSum == null ? 0 : todayOrderSum);

                saleVO.setYesterdayProdOrderNum(yesterdayProdNum == null ? 0 : yesterdayProdNum);
                saleVO.setYesterdayOrderNum(yesterdayNum == null ? 0 : yesterdayNum);
                saleVO.setYesterdayOrderSum(yesterdayOrderSum == null ? 0 : yesterdayOrderSum);

                saleVO.setCountCA(countCA);
                saleVO.setCountEnableCA(countEnableCA);
                saleVO.setBalance(totalRecharge - totalCost);
                saleVOList.add(saleVO);
            }

            return saleVOList;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<SaleVO> listSaleInfo() {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = this.usMapper.listSidByUid(uid);
        sidList.add(uid);
        if (sidList != null && sidList.size() != 0) {
            List<User> userList = this.userMapper.selectBatchIds(sidList);
            List<SaleVO> saleVOList = new ArrayList<>();

            for (User user : userList) {
                Integer sid = user.getId();
                Integer totalCost = this.vboxUserWalletMapper.getTotalCostByUid(sid);
                Integer totalCostNum = this.vboxUserWalletMapper.getTotalCostNumByUid(sid);
                Integer totalProdOrderNum = this.vboxUserWalletMapper.getTotalProdOrderNum(sid);

                Integer todayProdOrderNum = this.vboxUserWalletMapper.getTodayProdOrderNum(sid);
                Integer todayOrderNum = this.vboxUserWalletMapper.getTodayOrderNum(sid);
                Integer todayOrderSum = this.vboxUserWalletMapper.getTodayOrderSum(sid);

                Integer yesterdayProdNum = this.vboxUserWalletMapper.getYesterdayProdOrderNum(sid);
                Integer yesterdayNum = this.vboxUserWalletMapper.getYesterdayOrderNum(sid);
                Integer yesterdayOrderSum = this.vboxUserWalletMapper.getYesterdayOrderSum(sid);

                Integer countCA = this.cAccountMapper.countByUid(sid);
                Integer countEnableCA = this.cAccountMapper.countACEnableByUid(sid);
                Integer totalRecharge = this.vboxUserWalletMapper.getTotalRechargeByUid(sid);
                SaleVO saleVO = new SaleVO();
                saleVO.setId(sid);
                saleVO.setAccount(user.getAccount());
                saleVO.setNickname(user.getNickname());
                totalCost = totalCost == null ? 0 : totalCost;
                totalRecharge = totalRecharge == null ? 0 : totalRecharge;

                saleVO.setTotalCost(totalCost);
                saleVO.setTotalCostNum(totalCostNum == null ? 0 : totalCostNum);
                saleVO.setTotalNum(totalProdOrderNum == null ? 0 : totalProdOrderNum);

                saleVO.setTodayProdOrderNum(todayProdOrderNum == null ? 0 : todayProdOrderNum);
                saleVO.setTodayOrderNum(todayOrderNum == null ? 0 : todayOrderNum);
                saleVO.setTodayOrderSum(todayOrderSum == null ? 0 : todayOrderSum);

                saleVO.setYesterdayProdOrderNum(yesterdayProdNum == null ? 0 : yesterdayProdNum);
                saleVO.setYesterdayOrderNum(yesterdayNum == null ? 0 : yesterdayNum);
                saleVO.setYesterdayOrderSum(yesterdayOrderSum == null ? 0 : yesterdayOrderSum);

                saleVO.setCountCA(countCA);
                saleVO.setCountEnableCA(countEnableCA);
                saleVO.setBalance(totalRecharge - totalCost);
                saleVOList.add(saleVO);
            }

            return saleVOList;
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public ResultOfList listSaleCAccount(Integer status, String saleName, String acRemark, Integer page, Integer pageSize) {
        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = usMapper.listSidByUid(uid);

        if (saleName != null) {
            User account = userMapper.selectOne(new QueryWrapper<User>().eq("id", saleName));
            if (sidList != null && sidList.size() > 0 && account != null && sidList.contains(account.getId())) {
                sidList = Collections.singletonList(account.getId());
            }
        }

        pageSize = pageSize == null ? 20 : pageSize;
        page = page == null ? 0 : (page - 1) * pageSize;
        List<CAccount> caList = cAccountMapper.listACInUids(sidList, acRemark, status, page, pageSize);
        Integer count = cAccountMapper.countACInUids(sidList, status, page, pageSize);
        List<CAccountVO> acVOList = new ArrayList<>();
        for (CAccount ca : caList) {
            CAccountVO acv = CAccountVO.transfer(ca);
            CGatewayInfo gw = cGatewayMapper.getGateWayInfoByCIdAndGId(ca.getCid(), ca.getGid());
            acv.setC_channel_id(gw.getCChannel());
            acv.setC_channel_name(gw.getCGameName() + "-" + gw.getCChannelName());
            acv.setC_gateway_name(gw.getCGatewayName());
            acv.setAc_pwd(DesensitizedUtil.idCardNum(Base64.decodeStr(ca.getAcPwd()), 1, 1));

            //总充值
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(ca.getId());
            //今充值
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(ca.getId());
            //昨充值
            Integer yesterdayCost = vboxUserWalletMapper.getYesterdayOrderSumByCaid(ca.getId());//昨充值
            Integer beforeDayCost = vboxUserWalletMapper.getBeforeDayOrderSumByCaid(ca.getId());

            acv.setBefore_day_cost(beforeDayCost == null ? 0 : beforeDayCost);
            acv.setYesterday_cost(yesterdayCost == null ? 0 : yesterdayCost);
            acv.setToday_cost(todayCost == null ? 0 : todayCost);
            acv.setTotal_cost(totalCost == null ? 0 : totalCost);
            acv.setDaily_limit(ca.getDailyLimit());
            acv.setTotal_limit(ca.getTotalLimit());
            //
            User user = userMapper.selectOne(new QueryWrapper<User>().eq("id", ca.getUid()));
            acv.setSale_name(user.getAccount());
            acVOList.add(acv);
        }
        ResultOfList rs = new ResultOfList<>(acVOList, count);
        return rs;
    }

    @Override
    public int createSub(UserSubCreateOrUpdParam param) throws Exception {

        User user = new User();

        //check account
        String account = param.getAccount();
        if (null == account) throw new Exception("account is null!");
        Integer exist = userMapper.isExistAccount(account);
        if (null != exist) throw new Exception("user is exist!");

        // user
        user.setAccount(account);
        user.setNickname(RandomNameUtil.randomChineseName());
        user.setCreate_time(LocalDateTime.now());
        userService.save(user);

        // user ext
        UserExt userExt = new UserExt();
        userExt.setUid(user.getId());
        userExt.setGender(-1);
        userExtMapper.insert(userExt);

        // user - sub relation
        RelationUserSub us = new RelationUserSub();
        us.setUid(TokenInfoThreadHolder.getToken().getId());
        us.setSid(user.getId());
        usMapper.insert(us);

        // user - login
        UserLogin userLogin = new UserLogin();
        userLogin.setUid(user.getId());
        userLogin.setUsername(user.getAccount());
        userLogin.setCaptcha(param.getPass() != null
                ? Base32.encode(param.getPass()) : Base32.encode("123456"));
        userLogin.setLoginType(LoginEnum.ACCOUNT.getType());
        userLogin.setCreateTime(LocalDateTime.now());
        userLogin.setRemark("账户登陆");
        userLoginMapper.insert(userLogin);

        //user - dept
        RelationUserDept ud = new RelationUserDept();
        ud.setUid(user.getId());
        // 直接设置为 销售部
        Dept dept = deptMapper.selectOne(new QueryWrapper<Dept>().eq("dept_name", "核销业务部"));
        ud.setDid(dept.getId());
        udMapper.insert(ud);

        //user - role
        RelationUserRole ur = new RelationUserRole();
        ur.setUid(user.getId());
        // 直接设置为 子核销角色
        Role role = roleMapper.selectOne(new QueryWrapper<Role>().eq("role_value", "sub_sale"));
        ur.setRid(role.getId());
        urMapper.insert(ur);

        //auth
        UserAuth auth = new UserAuth();
        KeyPair rsa = SecureUtil.generateKeyPair("RSA");
        auth.setSecret(Base64.encode(rsa.getPrivate().getEncoded()));
        auth.setPub(Base64.encode(rsa.getPublic().getEncoded()));
        auth.setUid(user.getId());
        auth.setCreateTime(LocalDateTime.now());
        userAuthMapper.insert(auth);

        //

        return 0;
    }
}
