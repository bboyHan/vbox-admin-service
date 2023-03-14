package com.vbox.api;

import com.vbox.common.Result;
import com.vbox.service.channel.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private PayService payService;
    @GetMapping("/code/test")
    public ResponseEntity<Result<String>> t(String orderId) {
        String s = payService.orderWxHtml(orderId);
        return Result.ok(s);
    }
}
