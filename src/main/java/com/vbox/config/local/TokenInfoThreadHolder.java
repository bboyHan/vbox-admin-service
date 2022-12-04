package com.vbox.config.local;

import com.vbox.persistent.pojo.dto.TokenInfo;

public class TokenInfoThreadHolder {

    /**
     * 保存用户对象的ThreadLocal
     */
    private static final ThreadLocal<TokenInfo> TOKEN_INFO_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 添加当前登录用户方法
     */
    public static void addToken(TokenInfo token){
        TOKEN_INFO_THREAD_LOCAL.set(token);
    }

    public static TokenInfo getToken(){
        return TOKEN_INFO_THREAD_LOCAL.get();
    }

    public static void remove(){
        TOKEN_INFO_THREAD_LOCAL.remove();
    }

}
