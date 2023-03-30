package com.vbox.service.channel.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vbox.common.ResultOfList;
import com.vbox.common.util.RedisUtil;
import com.vbox.config.local.TokenInfoThreadHolder;
import com.vbox.persistent.entity.PAccount;
import com.vbox.persistent.entity.PAuth;
import com.vbox.persistent.entity.PayOrder;
import com.vbox.persistent.entity.Role;
import com.vbox.persistent.pojo.param.PAOverviewParam;
import com.vbox.persistent.pojo.param.PAccountParam;
import com.vbox.persistent.pojo.vo.PAOverviewVO;
import com.vbox.persistent.pojo.vo.PAccountVO;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayerService;
import com.vbox.service.task.Gee4Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PayerServiceImpl extends ServiceImpl<PAccountMapper, PAccount> implements PayerService {

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
    @Autowired
    private LocationMapper locationMapper;

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
    public ResultOfList<List<PAOverviewVO>> listPAccountOverview(PAOverviewParam queryParam) {

        QueryWrapper<PAccount> queryWrapper = new QueryWrapper<>();
        /*queryWrapper.in("ac_id", acIdList);
        if (StringUtils.hasLength(queryParam.getP_account())) {
            queryWrapper.eq("p_account", queryParam.getP_account());
        }
        if (StringUtils.hasLength(queryParam.getOrderId())) {
            queryWrapper.likeLeft("order_id", queryParam.getOrderId());
        }
        if (StringUtils.hasLength(queryParam.getOrderStatus())) {
            queryWrapper.eq("order_status", queryParam.getOrderStatus());
        }
        if (StringUtils.hasLength(queryParam.getCallbackStatus())) {
            queryWrapper.eq("callback_status", queryParam.getCallbackStatus());
        }
        if (StringUtils.hasLength(queryParam.getCChannelId())) {
            queryWrapper.eq("c_channel_id", queryParam.getCChannelId());
        }*/
        queryWrapper.orderByDesc("id");

        Page<PAccount> page = null;
        if (null != queryParam.getPage() && null != queryParam.getPageSize()) {
            page = new Page<>(queryParam.getPage(), queryParam.getPageSize());
        } else {
            page = new Page<>(1, 10);
        }

        Page<PAccount> paPage = pAccountMapper.selectPage(page, queryWrapper);


        List<PAOverviewVO> povList = new ArrayList<>();
        for (PAccount pa : paPage.getRecords()) {
            // 总产单
            Integer countOrder = pOrderMapper.countPOrderByPAID(pa.getPAccount());
            // 总成单
            Integer countPayed = pOrderMapper.countPOrderPayedByPAID(pa.getPAccount());
            // 昨天产单
            Integer countOrderYesterday = pOrderMapper.countPOrderYesterdayByPAID(pa.getPAccount());
            // 昨天成单
            Integer countPayedYesterday = pOrderMapper.countPOrderPayedYesterdayByPAID(pa.getPAccount());
            // 今天产单
            Integer countOrderToday = pOrderMapper.countPOrderTodayByPAID(pa.getPAccount());
            // 今天成单
            Integer countPayedToday = pOrderMapper.countPOrderPayedTodayByPAID(pa.getPAccount());

            // 总成单 金额
            Integer sumPayed = pOrderMapper.sumPOrderPayedByPAID(pa.getPAccount());
            // 昨天成单 金额
            Integer sumPayedYesterday = pOrderMapper.sumPOrderPayedYesterdayByPAID(pa.getPAccount());
            // 今天成单 金额
            Integer sumPayedToday = pOrderMapper.sumPOrderPayedTodayByPAID(pa.getPAccount());

            PAOverviewVO pov = new PAOverviewVO();
            pov.setPAccount(pa.getPAccount());
            pov.setPRemark(pa.getPRemark());
            pov.setCountOrder(countOrder == null ? 0 : countOrder);
            pov.setCountPayed(countPayed == null ? 0 : countPayed);
            pov.setCountOrderToday(countOrderToday == null ? 0 : countOrderToday);
            pov.setCountPayedToday(countPayedToday == null ? 0 : countPayedToday);
            pov.setCountPayedYesterday(countPayedYesterday == null ? 0 : countPayedYesterday);
            pov.setCountOrderYesterday(countOrderYesterday == null ? 0 : countOrderYesterday);

            pov.setSumPayed(sumPayed == null ? 0 : sumPayed);
            pov.setSumPayedToday(sumPayedToday == null ? 0 : sumPayedToday);
            pov.setSumPayedYesterday(sumPayedYesterday == null ? 0 : sumPayedYesterday);
            povList.add(pov);
        }

        ResultOfList rs = new ResultOfList(povList, (int) page.getTotal());
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
