package com.vbox.api;

import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.Result;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.pojo.param.GeeProdCodeParam;
import com.vbox.persistent.pojo.param.GeeVerifyParam;
import com.vbox.persistent.pojo.param.VOrderQueryParam;
import com.vbox.service.task.Gee4Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class Gee4Controller {

    @Autowired
    private Gee4Service gee4Service;

    @PostMapping("/code/order/create")
    public ResponseEntity<Result<Object>> createOrder(PayInfo payInfo) throws Exception {
//        payInfo.setChannel("weixin");
//        payInfo.setGateway("z01");
//        payInfo.setRecharge_type(6); //通宝type
//        payInfo.setRecharge_unit(15);
//        payInfo.setRepeat_passport("chenzhj11");
//        payInfo.setGame("jx3");
//        payInfo.setCk("xoyo=.....");
        Object order = gee4Service.createOrder(payInfo);
        return Result.ok(order);
    }

    @GetMapping("/test/test")
    public ResponseEntity<Result<Object>> test() throws Exception {
        Object order = gee4Service.capSecCode();
        return Result.ok(order);
    }

    @GetMapping("/code/cap")
    public ResponseEntity<Result<JSONObject>> cap() throws IOException {
        JSONObject rs = gee4Service.cap();
        return Result.ok(rs);
    }

    @PostMapping("/code/analysis")
    public ResponseEntity<Result<JSONObject>> analysis(@RequestBody JSONObject data){
        JSONObject rs = gee4Service.analysis(data);
        return Result.ok(rs);
    }

    @PostMapping("/code/verify")
    public ResponseEntity<Result<JSONObject>> verify(@RequestBody GeeVerifyParam param){
        JSONObject rs = gee4Service.verify(param);
        return Result.ok(rs);
    }

    @PostMapping("/code/prod")
    public ResponseEntity<Result<JSONObject>> prod(@RequestBody GeeProdCodeParam param){
        JSONObject rs = gee4Service.prodCode(param);
        return Result.ok(rs);
    }

    @PostMapping("/code/order/query")
    public ResponseEntity<Result<JSONObject>> queryOrder(@RequestBody VOrderQueryParam param){
        JSONObject rs = gee4Service.queryOrder(param);
        return Result.ok(rs);
    }
}
