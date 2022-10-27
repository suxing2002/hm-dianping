package com.hmdp.utils;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @Author GuoShuo
 * @Time 2022/10/15 18:08
 * @Version 1.0
 * @Description redis工具类, 可以应对缓存击穿 缓存穿透
 */
@Component
public class RedisUtils {
    @Resource
    private MutexLock mutexLock;
    @Resource
    private StringRedisTemplate template;
    private final static ExecutorService threadPool = Executors.newFixedThreadPool(20);

    /**
     * 从数据库中查询数据,并创建缓存,可以解决缓存穿透问题(没有解决击穿问题),适用于访问频率一般,且缓存重建较快的请求
     *
     * @param querySql    数据库查询数据语句
     * @param id          不重复的索引
     * @param cachePrefix 缓存key的前缀,为了使缓存分级
     * @param expireTime  超时时间
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> T createCache(Function<R, T> querySql, R id, String cachePrefix, Duration expireTime) {
        T queryResult = querySql.apply(id);
        String cacheKey = cachePrefix + id;
        if (queryResult == null) {
            template.opsForValue().set(cacheKey, "", expireTime);
            return null;
        }
        template.opsForValue().set(cacheKey, JSON.toJSONString(queryResult), expireTime);
        return queryResult;
    }

    /**
     * 查询缓存数据,适用于逻辑过期的问题
     * @param id
     * @param keyPrefix
     * @param <R>
     * @return 逻辑过期,理论数据库中数据与缓存存在性保持一致
     */
    public <R> RedisData queryCacheForRedisData(R id, String keyPrefix) {
        String cacheKey = keyPrefix + id;
        String cacheRedisData = template.opsForValue().get(cacheKey);
        if(!StringUtils.hasText(cacheRedisData)){
            return null;
        }
        return JSON.parseObject(cacheRedisData, RedisData.class);
    }

    public <T, R> T queryCacheForBean(R id, String keyPrefix , Class<T> type) {
        String cacheKey = keyPrefix + id;
        String cacheData = template.opsForValue().get(cacheKey);
        if (!StringUtils.hasText(cacheData)) {
            if ("".equals(cacheData)) {
                try {
                    return type.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
        return JSON.parseObject(cacheData , type);
    }

    /**
     * 从数据库中查询数据,并创建缓存,解决缓存穿透以及击穿问题,使用了互斥锁,适用于需要保证数据一致性的请求
     *
     * @param id
     * @param lockKeyPrefix
     * @param cacheKeyPrefix
     * @param expireTime
     * @param type
     * @param querySql
     * @param <T>
     * @param <R>
     * @return
     */
    public <T, R> T cacheThroughWithMutexLock(R id, String cacheKeyPrefix, String lockKeyPrefix, Duration expireTime, Class<T> type, Function<R, T> querySql) {
        String lockKey = lockKeyPrefix + id;
        T cache = queryCacheForBean(id, cacheKeyPrefix, type);
        if(cache != null){
            return cache;
        }
        try {
            while (!mutexLock.getLock(lockKey)) {
                T cacheData = queryCacheForBean(id, cacheKeyPrefix, type);
                if (cacheData != null) {
                    return cacheData;
                }
            }
            return createCache(querySql, id, cacheKeyPrefix, expireTime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            mutexLock.releaseLock(lockKey);
        }
    }

    /**
     * 查询缓存,如果缓存数据已过期则重新建立缓存(逻辑过期),适用于对数据一致性要求不高的请求
     * @param id
     * @param cacheKeyPrefix
     * @param lockKeyPrefix
     * @param expireSeconds
     * @param type
     * @param querySql
     * @return
     * @param <T>
     * @param <R>
     */
    public <T, R> T cacheThroughWithLogicExpire(R id, String cacheKeyPrefix, String lockKeyPrefix, Duration expireSeconds,Class<T> type, Function<R, T> querySql) {
        String lockKey = lockKeyPrefix + id;
        String cacheKey = cacheKeyPrefix + id;
        RedisData redisData = queryCacheForRedisData(id, cacheKeyPrefix);
        if(redisData == null){
            return null;
        }
        JSONObject jsonObject = (JSONObject) redisData.getData();
        T oldValue = jsonObject.to(type);
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){
            return oldValue;
        }
        if (mutexLock.getLock(lockKey)) {
            threadPool.submit(() -> {
                T queryResult = querySql.apply(id);
                RedisData cache = new RedisData(LocalDateTime.now().plusSeconds(expireSeconds.getSeconds()) , queryResult);
                template.opsForValue().set(cacheKey, JSON.toJSONString(cache));
            });
        }
        T newCache = queryCacheForBean(id, cacheKeyPrefix, type);
        if(newCache != null){
            return newCache;
        }
        return oldValue;
    }
}
