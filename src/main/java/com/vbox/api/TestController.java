package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.persistent.pojo.dto.PayInfo;
import com.vbox.persistent.pojo.param.PayInfoParam;
import com.vbox.service.channel.PayService;
import com.vbox.service.task.Gee4Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private PayService payService;
    @Autowired
    private Gee4Service gee4Service;
    @GetMapping("/code/test")
    public ResponseEntity<Result<String>> t(String orderId) {
        String s = payService.orderWxHtml(orderId);
        return Result.ok(s);
    }

//    @PostMapping("/code/jx3/cap")
    public ResponseEntity<Result<Object>> ynForPayload(@RequestBody PayInfoParam param) throws Exception {
        Object s = payService.ynForPayload(param);
        return Result.ok(s);
    }

}
