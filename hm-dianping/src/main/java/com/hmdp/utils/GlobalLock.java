package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Arrays;


/**
 * @Author GuoShuo
 * @Time 2022/10/18 15:29
 * @Version 1.0
 * @Description 分布式锁 , 基于redis + Lua脚本
 */
public class GlobalLock {
    private StringRedisTemplate redisTemplate;
    private final String LOCK_KEY_PREFIX = "global_key:";
    private static DefaultRedisScript<Boolean> unLockScript;
    private String lockKey;
    private String uuid;
    static {
        unLockScript = new DefaultRedisScript();
        unLockScript.setLocation(new ClassPathResource("luaScript/GlobalLock.lua"));
        unLockScript.setResultType(Boolean.class);
    }
    public GlobalLock(StringRedisTemplate redisTemplate ,String lockKey) {
        this.redisTemplate = redisTemplate;
        this.lockKey = lockKey;
    }
    public boolean tryLock(Duration expireTimeOfSeconds){
        uuid = UUID.randomUUID().toString();
        Boolean result = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY_PREFIX + lockKey, uuid , expireTimeOfSeconds);
        return Boolean.TRUE.equals(result);
    }
    public boolean unLock(){
        Boolean execute = redisTemplate.execute(unLockScript, Arrays.asList(LOCK_KEY_PREFIX + lockKey), uuid);
        return Boolean.TRUE.equals(execute);
    }
}
