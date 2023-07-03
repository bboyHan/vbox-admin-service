package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.persistent.pojo.param.TxPreAuthParam;
import com.vbox.service.channel.TxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

//    @PostMapping("/channel/order/pre_auth")
    public ResponseEntity<Result<String>> getPayUrlForTool(@RequestBody TxPreAuthParam authParam) throws Exception {
        String rl = txPayService.preAuth(authParam);
        return Result.ok(rl);
    }

}
