package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;

import static com.hmdp.utils.RedisConstants.LOCK_HOLDER_TTL;

/**
 * @Author GuoShuo
 * @Time 2022/10/14 21:08
 * @Version 1.0
 * @Description 基于redis的单线程特性 + setnx自定义互斥锁
 */
@Component
public class MutexLock {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 获取名称为 lockKey 的互斥锁,如果该锁以被获取,则返回false,否则返回true
     * 为了避免死锁问题,锁仅能被持有10s
     * lockKey相同才是同一把锁
     * @param lockKey
     * @return
     */
    public boolean getLock(String lockKey){
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "", Duration.ofSeconds(LOCK_HOLDER_TTL));
        return BooleanUtil.isTrue(absent);
    }
    /**
     * 释放名为lockKey的锁
     * @param lockKey
     * @return
     */
    public boolean releaseLock(String lockKey){
        Boolean delete = stringRedisTemplate.delete(lockKey);
        return BooleanUtil.isTrue(delete);
    }
}
