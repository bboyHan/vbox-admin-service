package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base32;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.enums.LoginEnum;
import com.vbox.common.util.RandomNameUtil;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.*;
import com.vbox.persistent.pojo.dto.CGatewayInfo;
import com.vbox.persistent.pojo.param.UserSubCreateOrUpdParam;
import com.vbox.persistent.pojo.vo.CAccountVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.SaleService;
import com.vbox.service.system.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public List listSaleInfo() {

        Integer uid = TokenInfoThreadHolder.getToken().getId();
        List<Integer> sidList = usMapper.listSidByUid(uid);

        List<CAccount> caList = cAccountMapper.listACInUids(sidList);
        List<CAccountVO> acVOList = new ArrayList<>();
        for (CAccount ca : caList) {
            CAccountVO acv = CAccountVO.transfer(ca);
            CGatewayInfo gw = cGatewayMapper.getGateWayInfoByCIdAndGId(ca.getCid(), ca.getGid());
            acv.setC_channel_id(gw.getCChannel());
            acv.setC_channel_name(gw.getCGameName() + "-" + gw.getCChannelName());
            acv.setC_gateway_name(gw.getCGatewayName());
            acv.setAc_pwd(DesensitizedUtil.idCardNum(Base64.decodeStr(ca.getAcPwd()), 1, 1));

            //cost
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(ca.getId());
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(ca.getId());
            acv.setToday_cost(todayCost == null ? 0 : todayCost);
            acv.setTotal_cost(totalCost == null ? 0 : totalCost);

            //
            userMapper.selectOne(new QueryWrapper<User>().eq("", ca.getUid()));
            acVOList.add(acv);
        }

        return null;
    }

    @Override
    public List listSaleCAccount() {
        return null;
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
