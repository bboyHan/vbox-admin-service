package com.vbox.test;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson2.JSONObject;

public class WMSJTest {

    public static void main(String[] args) {

        String url = "https://pay.wanmei.com/new/newpay.do?op=pay&gametype=1&exchange=150%7C%E5%88%86%E9%92%9F&username=16257918784&username2=16257918784&zone=20&paytype=402&payway=yinhangka&mobery=&sptype=on&cardnumber=&cardpasswd=&sxcardnumber=&sxcardpasswd=&ivrcardnumber=&ivrcardpasswd=&dxcardnumber=&dxcardpasswd=&rand=&money=6";
        String resp = HttpRequest.post(url).execute()
                .body();

        System.out.println(resp);

        JSONObject jsonObject = JSONObject.parseObject(resp);

        String ordernumber = jsonObject.getString("ordernumber");
        String redirectUrl = jsonObject.getString("redirectUrl");
        String status = jsonObject.getString("status");

        System.out.println(ordernumber);
        System.out.println(redirectUrl);
        System.out.println(status);
    }
}
