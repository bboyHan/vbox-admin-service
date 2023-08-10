package com.vbox.service.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.constant.CommonConstant;
import com.vbox.common.util.RedisUtil;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.entity.User;
import com.vbox.persistent.repo.*;
import com.vbox.service.channel.PayService;
import com.vbox.service.channel.impl.Gee4Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

//@Component
@Slf4j
public class OrderBalanceTask {

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
    private ChannelPreMapper channelPreMapper;

    @Scheduled(cron = "0 */1 * * * ?")
    public void handleUserBalance() {
        List<User> users = userMapper.selectList(null);

        for (User user : users) {
            Integer uid = user.getId();
            // 总账户充值
            Integer totalRecharge = vboxUserWalletMapper.getTotalRechargeByUid(uid);

            // 总订单充值（花费）
            Integer totalCost = vboxUserWalletMapper.getTotalCostByUid(uid);

            totalRecharge = totalRecharge == null ? 0 : totalRecharge;
            totalCost = totalCost == null ? 0 : totalCost;
            // 总余额
            int balance = totalRecharge - totalCost;

            if (balance <= 0) {
                cAccountMapper.stopByUid("系统监测余额不足，请联系管理员确认", uid);
            }

            if (balance > 500 && balance < 5000) {
                cAccountMapper.startByUid("账户余额不足5000，请注意使用", uid);
            }

            if (balance > 5000) {
                cAccountMapper.startByUid("账户可正常使用，请随时查看状态", uid);
            }
        }

        QueryWrapper<CAccount> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("status", 1);
        queryWrapper.eq("sys_status", 1);
        List<CAccount> acList = cAccountMapper.selectList(queryWrapper);

        for (CAccount ca : acList) {
            Integer dailyLimit = ca.getDailyLimit();
            Integer totalLimit = ca.getTotalLimit();

            Integer acID = ca.getId();
            Integer todayCost = vboxUserWalletMapper.getTodayOrderSumByCaid(acID);
            Integer totalCost = vboxUserWalletMapper.getTotalCostByCaid(acID);

            if (dailyLimit != null && dailyLimit > 0 && todayCost != null && todayCost >= dailyLimit) {
                cAccountMapper.stopByCaId("已超出日内限额，账号关闭", acID);
                channelPreMapper.stopByACID(ca.getAcid());
//                Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + "*");
//                for (String key : keys) {
//                    log.warn("关闭账号时，清理通道keys: {}", key);
//                    redisUtil.del(key);
//                }
            }

            if (totalLimit != null && totalLimit > 0 && totalCost != null && totalCost >= totalLimit) {
                cAccountMapper.stopByCaId("已超出总限额控制，账号关闭", acID);
                channelPreMapper.stopByACID(ca.getAcid());
//                Set<String> keys = redisUtil.keys(CommonConstant.CHANNEL_ACCOUNT_QUEUE + "*");
//                for (String key : keys) {
//                    log.warn("关闭账号时，清理通道keys: {}", key);
//                    redisUtil.del(key);
//                }
            }
        }
    }

}
