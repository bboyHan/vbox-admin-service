package com.vbox.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vbox.common.Result;
import com.vbox.persistent.entity.CAccount;
import com.vbox.persistent.pojo.dto.TxWaterList;
import com.vbox.persistent.pojo.param.TxPreAuthParam;
import com.vbox.persistent.repo.CAccountMapper;
import com.vbox.service.channel.TxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api")
@Slf4j
public class TxController {
    @Autowired
    private TxPayService txPayService;
//    @PostMapping("/channel/order/pre_auth")
    public ResponseEntity<Result<String>> preAuth(@RequestBody TxPreAuthParam authParam) throws Exception {
        String rl = txPayService.preAuth(authParam);
        return Result.ok(rl);
    }

    @Autowired
    CAccountMapper cAccountMapper;
    @PutMapping("/channel/order/abcxxx")
    public ResponseEntity<Result<Object>> getPayUrlForTool(TxPreAuthParam authParam) throws Exception {
        List<CAccount> randomTempList = cAccountMapper.selectList(new QueryWrapper<CAccount>()
                .eq("status", 1)
                .eq("sys_status", 1)
                .eq("cid", 7)
        );

        Set<TxWaterList> rl = new HashSet<>();
        // 使用HashMap来保存相同充值金额的充值账号
        Map<Integer, List<String>> map = new HashMap<>();

        for (CAccount cAccount : randomTempList) {
            String openID = cAccount.getAcPwd();
            String openKey = cAccount.getCk();
            List<TxWaterList> txWaterList = txPayService.queryOrderBy30(openID, openKey);
            log.warn("c- {}",txWaterList);
            rl.addAll(txWaterList);
        }

        LocalDateTime now = LocalDateTime.now();
        // 遍历rechargeList进行充值金额的筛选
        for (TxWaterList recharge : rl) {
            Integer payAmt = recharge.getPayAmt() / 100;
            String provideID = recharge.getProvideID();
            long payTime = recharge.getPayTime();
            Instant instant = Instant.ofEpochSecond(payTime);
            LocalDateTime parse = LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Shanghai"));
            LocalDateTime pre30min = now.plusMinutes(-30);
            // 如果该充值金额已存在于结果集中，则将充值账号添加进对应的列表中
            if (parse.isAfter(pre30min)) {
                List<String> accountList = map.computeIfAbsent(payAmt, k -> new ArrayList<>());
                accountList.add(provideID);
            }
        }

        return Result.ok(map);
    }

}
