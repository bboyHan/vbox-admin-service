package com.vbox.service.task;

import org.springframework.data.redis.core.RedisTemplate;

public class ConsumerThread extends Thread {
    private RedisTemplate redisTemplate;
    public ConsumerThread(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }
    @Override
    public void run(){
        RedisDelayQueue queue = new RedisDelayQueue(redisTemplate,"下单未付款,1min后自动取消!");
        queue.listenDelayLoop();
    }
}
