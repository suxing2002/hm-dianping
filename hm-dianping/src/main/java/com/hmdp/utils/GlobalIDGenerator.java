package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.hmdp.utils.SystemConstants.OFFSET_VALUE;

/**
 * @Author GuoShuo
 * @Time 2022/10/16 18:59
 * @Version 1.0
 * @Description 全局唯一Id生成器
 */
@Component
public class GlobalIDGenerator {
    private StringRedisTemplate redisTemplate;

    public GlobalIDGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取对应事务的全局Id,不同事务有着不同的ID
     * 1位符号 + 31位时间戳 + 32位序列号
     * @param keyPrefix
     * @return
     */
    public Long getId(String keyPrefix){
        long nowSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.of(SystemConstants.ZONE_OFFSET));
        long idPrefix = nowSeconds - SystemConstants.BEGIN_SECONDS;
        Long idSuffix = redisTemplate.opsForValue().increment("IncrementKey:" + keyPrefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd")));
        return idPrefix << OFFSET_VALUE | idSuffix;
    }

    public static void main(String[] args) {
        long oldSeconds = LocalDateTime.of(2022, 1, 1, 0, 0).toEpochSecond(ZoneOffset.of(SystemConstants.ZONE_OFFSET));
        System.out.println(oldSeconds);
    }
}
