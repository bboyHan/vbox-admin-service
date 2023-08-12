package com.vbox.common.util;

import com.alibaba.fastjson2.JSONObject;
import com.vbox.common.constant.CommonConstant;
import com.vbox.persistent.pojo.dto.SecCode;
import com.vbox.service.task.DelayTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisUtil {

    private RedisTemplate<String, Object> redisTemplate;

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    //=============================proxy============================
    // 设置键的值
    public void setKey(String ip, int port) {
        String key = generateKey(ip, port);
        redisTemplate.opsForValue().set(key, 0, 120, TimeUnit.SECONDS); // 设置初始值为0，过期时间为120秒
    }

    // 模糊匹配获取键集合
    public Set<String> getKeysByPattern(String pattern) {
        return redisTemplate.keys(pattern);
    }

    // 判断键是否可用
    public boolean isKeyAvailable(String key) {
        Integer count = (Integer) redisTemplate.opsForValue().get(key);
        log.warn("当前key ： {}， 消耗 count： {}", key, count);
        boolean flag = count != null && count < 90; // 判断计数是否小于40
        if (!flag) {
            deleteKey(key);
            log.warn("够{}次了，干掉key: {}", count, key);
        }
        return flag;
    }

    // 增加键的计数
    public void incrementCount(String key) {
        redisTemplate.opsForValue().increment(key, 1); // 计数加1
    }

    // 删除键
    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    // 生成带前缀的键
    private String generateKey(String ip, int port) {
        return "proxy_key," + ip + ":" + port;
    }

    //=============================common============================

    public void pub(Object msg) {
        try {
            this.redisTemplate.convertAndSend("vbox_order:message", msg.toString());
        } catch (Exception var3) {
            System.out.println("redis 发布消息 err" + var3.getMessage());
        }
    }


    public Set<String> keys(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        return keys;
    }

    public void clearExpireGee4Keys() {
        String key = CommonConstant.CHANNEL_ACCOUNT_GEE;
        long currentTime = System.currentTimeMillis();
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.removeRangeByScore(key, 0, currentTime - 60000);
    }

    //============================= queue sec ============================
    public void addSecCode(SecCode secCode) {
        try {
            long currentTime = System.currentTimeMillis();
            String key = CommonConstant.CHANNEL_ACCOUNT_GEE;
            String value = JSONObject.toJSONString(secCode);
            ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
            zSetOperations.add(key, value, currentTime);
            zSetOperations.removeRangeByScore(key, 0, currentTime - 20000);
            zSetOperations.removeRange(key, 0, -100 - 1);
        } catch (Exception e) {
            log.error("添加sec code 失败", e);
        }
    }

    public int sizeSecCode() {
        try {
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            String key = CommonConstant.CHANNEL_ACCOUNT_GEE;
            Long zc = zSetOps.zCard(key);
            if (zc != null) {
                return zc.intValue();
            } else return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public SecCode popSecCode() {
        try {
            ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
            String key = CommonConstant.CHANNEL_ACCOUNT_GEE;
            Set<Object> earliest = zSetOps.range(key, 0, 0);
            if (earliest == null) {
                return null;
            } else {
                Object value = earliest.iterator().next();
                String ele = value.toString();
                SecCode secCode = JSONObject.parseObject(ele, SecCode.class);
                zSetOps.remove(key, value);

                log.info("当前size: {}, 取到元素:  sec => {}", sizeSecCode(), secCode);
                return secCode;
            }
        } catch (Exception e) {
            log.warn("redis sec code 没元素");
            return null;
        }
    }

    private static final String LOCK_KEY = "account_lock";
    private static final int LOCK_EXPIRE_SECONDS = 10;
    private static final int MAX_WAIT_TIME_SECONDS = 30;
    public boolean acquireLock() {
        // 使用SETNX命令尝试获取锁，设置过期时间防止死锁
        Boolean success = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
        return success != null && success;
    }

    public void releaseLock() {
        // 释放锁
        redisTemplate.delete(LOCK_KEY);
    }

    public boolean waitForLock() {
        // 业务等待直到获取到锁或超时
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < MAX_WAIT_TIME_SECONDS * 1000) {
            if (acquireLock()) {
                return true;
            } else {
                try {
                    Thread.sleep(100); // 适当的休眠时间，避免过多消耗CPU
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
    //============================= queue sec end ============================

    /**
     * 指定缓存失效时间
     *
     * @param key  键
     * @param time 时间(秒)
     * @return
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据key 获取过期时间
     *
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
            }
        }
    }

    //============================String=============================

    /**
     * 普通缓存获取
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几(大于0)
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几(小于0)
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }
    //================================Map=================================

    /**
     * HashGet
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取hashKey对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return true 成功 false 失败
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     *
     * @param key  键
     * @param map  对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @param time  时间(秒)  注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除hash表中的值
     *
     * @param key  键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断hash表中是否有该项的值
     *
     * @param key  键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return
     */
    public double hincr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return
     */
    public double hdecr(String key, String item, double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

    //============================set=============================

    /**
     * 根据key获取Set中的所有值
     *
     * @param key 键
     * @return
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object sGetOne(String key) {
        try {
            return redisTemplate.opsForSet().randomMember(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据value从一个set中查询,是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入set缓存
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param time   时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取set缓存的长度
     *
     * @param key 键
     * @return
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除值为value的
     *
     * @param key    键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    //===============================list=================================

    /**
     * 获取list缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束  0 到 -1代表所有值
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取list缓存的长度
     *
     * @param key 键
     * @return
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 通过索引 获取list中的值
     *
     * @param key   键
     * @param index 索引  index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     * @return
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     */
    public boolean lPush(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @return
     */
    public boolean lPushAll(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().leftPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将list放入缓存
     *
     * @param key   键
     * @param value 值
     * @param time  时间(秒)
     * @return
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) expire(key, time);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引修改list中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 移除N个值为value
     *
     * @param key   键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean zAdd(String queueName, DelayTask<?> delayTask, long delayTime) {
        Boolean add = null;
        try {
            add = redisTemplate.opsForZSet().add(queueName, JSONObject.toJSONString(delayTask), System.currentTimeMillis() + delayTime);
            return Boolean.TRUE.equals(add);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Set<?> zGet(String queueName) {
        Set<?> objects = null;
        try {
            objects = redisTemplate.opsForZSet().rangeByScore(queueName, 0, System.currentTimeMillis(), 0, 1);
            return objects;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean zRemove(String queueName, Object it) {
        Long remove;
        try {
            remove = redisTemplate.opsForZSet().remove(queueName, JSONObject.toJSONString(it));
            if (remove == null) return false;
            return remove > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean lPush(String queueName, Object it) {
        Long remove;
        try {
            remove = redisTemplate.opsForList().leftPush(queueName, JSONObject.toJSONString(it));
            if (remove == null) return false;
            return remove > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Object rPop(String queueName) { //如果队列里没用元素，等10s， 还是没有则退出返回null
        Object ele;
        try {
            ele = redisTemplate.opsForList().rightPop(queueName, 10, TimeUnit.SECONDS);
            return ele;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

