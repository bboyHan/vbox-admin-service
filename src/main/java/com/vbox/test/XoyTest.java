package com.vbox.test;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;

public class XoyTest {
    public static void main(String[] args) {

        //https://security.seasungame.com/security_extend_server/helper/balance/queryBalance?gameCode=jx3&account=chu8038873&accountType=&zoneCode=z05&SN=98710485560&remark=&sign=D8199F85E045C85CAC6373153EFF6620
        String body = HttpRequest.post("https://security.seasungame.com/security_extend_server/helper/balance/queryBalance?gameCode=jx3&account=chu8038873&accountType=&zoneCode=z05&SN=98710485560&remark=&sign=D8199F85E045C85CAC6373153EFF6620")
                .execute().body();
        JSONObject jsonObject = JSONObject.parseObject(body);
        JSONObject data = jsonObject.getJSONObject("data");
        Integer balance = data.getInteger("leftCoins");
        System.out.println(balance);
    }
}
