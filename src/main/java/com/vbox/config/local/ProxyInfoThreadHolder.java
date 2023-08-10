package com.vbox.config.local;

import com.vbox.persistent.pojo.dto.ProxyInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyInfoThreadHolder {

    /**
     * 保存用户对象的ThreadLocal
     */
    private static final ThreadLocal<ProxyInfo> TOKEN_INFO_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 添加当前登录用户方法
     */
    public static void addProxy(ProxyInfo token) {
        TOKEN_INFO_THREAD_LOCAL.set(token);
        log.info("当前proxy : {}", token);
    }

    public static ProxyInfo getProxy() {
        return TOKEN_INFO_THREAD_LOCAL.get();
    }

    public static void remove() {
        TOKEN_INFO_THREAD_LOCAL.remove();
    }

    public static String getAddress() {
        return TOKEN_INFO_THREAD_LOCAL.get().getIpAddr() + ":" + TOKEN_INFO_THREAD_LOCAL.get().getPort();
    }

    public static String getIpAddr() {
        return TOKEN_INFO_THREAD_LOCAL.get().getIpAddr();
    }

    public static int getPort() {
        return TOKEN_INFO_THREAD_LOCAL.get().getPort();
    }
}
