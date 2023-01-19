package com.vbox.api;

import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.Result;
import com.vbox.service.task.Gee4Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class Gee4Controller {

    @Autowired
    private Gee4Service gee4Service;

    @GetMapping("/cap")
    public ResponseEntity<Result<JSONObject>> cap() throws IOException {
        JSONObject rs = gee4Service.cap();
        return Result.ok(rs);
    }
}
