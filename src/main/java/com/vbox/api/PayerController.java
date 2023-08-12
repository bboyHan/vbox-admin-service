package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.persistent.pojo.param.PAEnableParam;
import com.vbox.persistent.pojo.param.PAOverviewParam;
import com.vbox.persistent.pojo.param.PAccountParam;
import com.vbox.persistent.pojo.vo.PAOverviewVO;
import com.vbox.persistent.pojo.vo.PAccountVO;
import com.vbox.persistent.repo.PAccountMapper;
import com.vbox.persistent.repo.POrderMapper;
import com.vbox.service.channel.PayerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class PayerController {

    @Autowired
    private PayerService payerService;
    @Autowired
    private PAccountMapper pAccountMapper;
    @Autowired
    private POrderMapper pOrderMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    public boolean customerThread = false;

    @PostMapping("/channel/pac")
    public ResponseEntity<Result<Integer>> createPAccount(@RequestBody PAccountParam pAccountParam) {
        int rl = 0;
        try {
            rl = payerService.createPAccount(pAccountParam);
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(rl);
    }

    @GetMapping("/channel/pac")
    public ResponseEntity<Result<ResultOfList<List<PAccountVO>>>> listPAccount() {
        ResultOfList<List<PAccountVO>> rl = null;
        try {
            rl = payerService.listPAccount();
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(rl);
    }

    @PutMapping("/channel/pac/{pid}")
    public ResponseEntity<Result<Integer>> updPAccount(@PathVariable(value = "pid", required = false) Integer pid,
                                                       @RequestBody PAccountParam pAccountParam) {
        int rl = 0;
        try {
            rl = payerService.updPAccount(pid, pAccountParam);
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(rl);
    }

    @DeleteMapping("/channel/pac/{pid}")
    public ResponseEntity<Result<Integer>> delPAccount(@PathVariable Integer pid) {
        int rl = 0;
        try {
            rl = payerService.delPAccount(pid);
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(rl);
    }

    @PutMapping("/channel/pac/enable")
    public ResponseEntity<Result<Integer>> enablePAccount(@RequestBody PAEnableParam param) {
        int rl = 0;
        try {
            System.out.println(param);
            return Result.ok(rl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.ok(rl);
    }

    @GetMapping("/channel/pac/overview")
    public ResponseEntity<Result<ResultOfList<List<PAOverviewVO>>>> listPAccountOverview(PAOverviewParam param) throws Exception {
        ResultOfList<List<PAOverviewVO>> rl = payerService.listPAccountOverview(param);
        return Result.ok(rl);
    }

}
