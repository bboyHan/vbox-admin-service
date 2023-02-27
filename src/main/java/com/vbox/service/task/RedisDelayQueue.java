package com.vbox.service.task;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Set;

@SuppressWarnings("unchecked")
@Slf4j
public class RedisDelayQueue<T> {
    /**
     * 延迟队列名称
     */
    private String delayQueueName = "delay_queue:order";

    private RedisTemplate redisTemplate;
 
    // 传入redis客户端操作
    public RedisDelayQueue(RedisTemplate redisTemplate, String delayQueueName)
    {
        this.redisTemplate = redisTemplate;
        this.delayQueueName = delayQueueName;
    }

    /**
     * 设置延迟订单
     */
    public boolean setDelayTasks(T msg, long delayTime) {
        DelayTask<T> delayTask = new DelayTask<>();
        delayTask.setId(IdUtil.randomUUID());
        delayTask.setTask(msg);
        Boolean addResult = redisTemplate.opsForZSet().add(delayQueueName, JSONObject.toJSONString(delayTask), System.currentTimeMillis() + delayTime);
        if(addResult)
        {
            System.out.println("添加任务成功！"+JSONObject.toJSONString(delayTask)+"当前时间为"+ LocalDateTime.now());
            return true;
        }
        return false;
    }

    /**
     * 监听超时订单
     */
    public void listenDelayLoop() {
//        System.out.println("线程名称:"+ Thread.currentThread().getName());
//        System.out.println("线程id:"+ Thread.currentThread().getId());
        while (true) {
            // 获取一个到点的消息
            Set<String> set = redisTemplate.opsForZSet().rangeByScore(delayQueueName, 0, System.currentTimeMillis(), 0, 1);
 
            // 如果没有，就等等
            if (set == null || set.isEmpty()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // 继续执行
                continue;
            }
            // 获取具体消息的key
            String it = set.iterator().next();
            // 删除成功
            if (redisTemplate.opsForZSet().remove(delayQueueName, it) > 0) {
                // 拿到任务
                DelayTask delayTask = JSONObject.parseObject(it, DelayTask.class);
                // 后续处理
                System.out.println("消息到期;"+delayTask.getTask().toString()+",到期时间为"+ LocalDateTime.now());
            }
        }
    }
}
