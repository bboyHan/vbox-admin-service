package com.vbox.common.util;

import com.vbox.common.ExpireQueue;
import com.vbox.persistent.pojo.dto.SecCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SecCodeUtil {

    public static ExpireQueue<SecCode> secQueue = new ExpireQueue<>();

    public static void add(SecCode ele) {
        secQueue.add(ele);
    }

    public static int size() {
        return secQueue.size();
    }

    public static SecCode poll() {
        SecCode poll = secQueue.poll();
        log.info("当前size: {}, 取到元素:  sec => {}", secQueue.size(), poll);
        return poll;
    }
}
