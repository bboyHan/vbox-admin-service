package com.vbox.api;

import cn.hutool.core.util.IdUtil;
import com.vbox.common.Result;
import com.vbox.common.ResultOfList;
import com.vbox.common.util.CommonUtil;
import com.vbox.persistent.pojo.param.*;
import com.vbox.persistent.pojo.vo.*;
import com.vbox.persistent.repo.PAccountMapper;
import com.vbox.persistent.repo.POrderMapper;
import com.vbox.service.channel.PayService;
import com.vbox.service.task.ConsumerThread;
import com.vbox.service.task.RedisDelayQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.SortedMap;

@RestController
@RequestMapping("/api")
@Slf4j
public class PayController {

    @Autowired
    private PayService payService;
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
            rl = payService.createPAccount(pAccountParam);
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
            rl = payService.listPAccount();
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
            rl = payService.updPAccount(pid, pAccountParam);
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
            rl = payService.delPAccount(pid);
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

    @PostMapping("/channel/order/pre_auth")
    public ResponseEntity<Result<String>> preAuth(@RequestBody OrderPreAuthParam authParam) throws Exception {
        String rl = payService.preAuth(authParam);
        return Result.ok(rl);
    }

    @PostMapping("/channel/order/create")
//    @AccessLimit(maxCount = 30)
    public ResponseEntity<Result<Object>> createOrder(@RequestBody OrderCreateParam orderCreateParam) throws Exception {
        Object rl = payService.createOrder(orderCreateParam);
        return Result.ok(rl);
    }

    @PostMapping("/test/callback")
    public ResponseEntity<Result<Object>> testCallback(@RequestBody Object param) throws Exception {
        log.info("test callback success, {}", param);
        return Result.ok(param);
    }

    @GetMapping("/sys/order")
    public ResponseEntity<Result<Object>> listOrder() {
        Object rl = payService.listOrder();
        return Result.ok(rl);
    }

    @PostMapping("/channel/order/query")
//    @AccessLimit(maxCount = 100)
    public ResponseEntity<Result<OrderQueryVO>> queryOrder(@RequestBody OrderCreateParam orderCreateParam) throws Exception {
        OrderQueryVO rl = payService.queryOrderToP(orderCreateParam);

        return Result.ok(rl);
    }

    @GetMapping("/channel/order/create/test/{num}")
    public ResponseEntity<Result<Object>> orderTest(@PathVariable Integer num, String acid, String channel) throws Exception {
        Object rl = payService.createTestOrder(num, acid, channel);
        return Result.ok(rl);
    }

    @GetMapping("/channel/order/callback/test/{orderId}")
    public ResponseEntity<Result<Object>> orderCallback(@PathVariable String orderId) throws Exception {

        String body = payService.testOrderCallback(orderId);
        return Result.ok(body);
    }

    @GetMapping({"/channel/order/queryAndCallback/{orderId}"})
    public ResponseEntity<Result<Object>> queryAndCallback(@PathVariable String orderId) throws Exception {
        OrderQueryVO rs = payService.queryAndCallback(orderId);
        return Result.ok(rs);
    }

    @GetMapping("/channel/order/code/{orderId}")
    public ResponseEntity<Result<Object>> orderQuery(@PathVariable String orderId) throws Exception {
        PayOrderCreateVO body = payService.orderQuery(orderId);
        return Result.ok(body);
    }

    @GetMapping("/channel/order/code/wx/{orderId}")
    public ResponseEntity<Result<Object>> orderWxHtml(@PathVariable String orderId) throws Exception {
        String body = payService.orderWxHtml(orderId);
        return Result.ok(body);
    }

    @GetMapping("/channel/order/task")
    public Object createDelayTasks() {
        String msg = "订单号:" + 111 + ",下单时间:" + LocalDateTime.now();
        RedisDelayQueue queue = new RedisDelayQueue(redisTemplate, "下单未付款,1min后自动取消!");
        boolean result = queue.setDelayTasks(msg, 10000);

        if (!customerThread) {
            customerThread = true;
            ConsumerThread thread = new ConsumerThread(redisTemplate);
            thread.start();
        }
        if (result) {
            return Result.ok("下单成功,未付款,1min后自动取消订单!");
        }
        return Result.error("下单失败!");
    }


//    @GetMapping("/channel/order/test/{a}")
//    public Object test(@PathVariable String a) {
//        RLock lock = redissonClient.getLock("redisLock_" + a);
//        try {
//            lock.lock(3, TimeUnit.SECONDS);
//            System.out.println("锁了" + a);
//            Thread.sleep(10000);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                lock.unlock();
//                System.out.println("正常解锁了" + a);
//            } catch (IllegalMonitorStateException e) {
//                System.out.println("锁已经释放过了" + e.getMessage());
//            }
//        }
//        return null;
//    }


}
