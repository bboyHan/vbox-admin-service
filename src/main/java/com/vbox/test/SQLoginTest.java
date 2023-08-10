package com.vbox.test;

import cn.hutool.http.HttpRequest;
import com.vbox.config.local.ProxyInfoThreadHolder;

public class SQLoginTest {

    public static void main(String[] args) {

//        String resp = HttpRequest.get("http://101.89.120.162:555/api/pay?username=18210889498&amount=1000&cookie=sdo_dw_track=RmT7rL1euukxlRroUkngLw==; CAS_LOGIN_STATE=1; SECURE_CAS_LOGIN_STATE=1; nsessionid=3d6fdc99ce0d0ec4cc213df707f04a6e&proxy=49.69.228.201:40029")
//                .execute().body();

        String payReqUrl = "http://101.89.120.162:555/api/pay";

        String resp = HttpRequest.get(payReqUrl)
                .form("username", "18210889498")
                .form("amount", "10201")
                .form("cookie", "sdo_dw_track=Rma7rL1euukxlRroUkngLw==; CAS_LOGIN_STATE=1; SECURE_CAS_LOGIN_STATE=1; nsessionid=3d6fdc99ce0d0ec4cc213df707f04a6e")
                .form("proxy", "49.69.228.201:40029")
                .execute()
                .body();

        System.out.println(resp);
    }
}
