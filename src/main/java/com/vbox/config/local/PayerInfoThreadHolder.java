package com.vbox.config.local;

import com.vbox.persistent.pojo.dto.PayerInfo;

public class PayerInfoThreadHolder {

    private static final ThreadLocal<PayerInfo> TOKEN_INFO_THREAD_LOCAL = new ThreadLocal<>();

    public static void addPayer(PayerInfo token){
        TOKEN_INFO_THREAD_LOCAL.set(token);
    }

    public static PayerInfo getPayerInfo(){
        return TOKEN_INFO_THREAD_LOCAL.get();
    }

    public static void remove(){
        TOKEN_INFO_THREAD_LOCAL.remove();
    }

}
