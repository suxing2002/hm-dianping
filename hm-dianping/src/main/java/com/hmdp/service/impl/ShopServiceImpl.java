package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.MutexLock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.xml.crypto.Data;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ShopMapper shopMapper;
    @Resource
    private MutexLock mutexLock;
    /**
     * 缓存穿透:当请求所请求的数据是数据库中不存在时,会出现每次请求均会穿过redis打在数据库上,存在数据库崩溃的情况
     * 解决:
     * 1.在redis中存储对应的空数据
     * 该方法虽然有效的解决的缓存穿透问题,但是会出现内存大量使用的情况,且会小概率出现数据存在但请求为空的情况
     * 2.使用布隆过滤:
     * 很大程度上解决了缓存穿透问题,但是依然存在穿透问题(hash冲突引起的漏洞),并且删除数据困难(也是因为hash冲突),相对于1方法不会出现内存大量使用的情况
     * 缓存击穿:对于热点key,在高并发情况下且缓存难以重建(重建较慢),失效会出现大量请求直接击中数据库
     * 解决:
     * 1.通过互斥锁解决,当一个线程检测到缓存失效时,得到互斥锁,并重新建立缓存 其余线程访问缓存时,由于失效,也会去重新建立缓存,
     * 由于锁,剩余线程会休眠并不断执行 检测缓存是否已经重建 若以重建返回数据 没有重建,获得锁,重建缓存
     * 2.通过逻辑过期,在建立缓存时不设置对应的过期时间,而是额外保存一个字段值(字段值为现在的时间 + 要保存的时间),这样数据就不会出现过期删除
     * 每次请求时,都会检查数据是否已过期,如果过期,额外创建一个线程用来缓存的重建(也要加一个锁,不然就会出现产生大量线程的情况),请求的线程直接返回旧数据
     * 两种方法均可以解决缓存击穿的问题,1.方法会出现大量线程等待的问题(有几率出现死锁),但是数据的一致性良好,2.方法由于返回的是旧数据,可能会出现数据不一致的情况,但是不会出现线程等待的问题
     * 最终结果: 1 + 2
     * @param id
     * @return
     */
    @Override
    public Shop getShopDetailById(Long id) {
        //redis缓存查询店铺信息
        Shop cacheData = getCacheData(id);
        if(cacheData != null){
            return cacheData;
        }

        //高并发 且 缓存难以重建 容易出现缓存击穿
        //1.根据互斥锁解决
//        try {
//            while(!mutexLock.getLock(LOCK_SHOP_KEY + id)){
//                //没拿到锁一直循环休眠 + 检测缓存是否以重建 +试图获取锁(缓存没有重建获取锁进行重建)
//                Thread.currentThread().sleep(200);
//                cacheData = getCacheData(id);
//                //缓存已被重建,查询数据并返回
//                if(cacheData != null){
//                    return cacheData;
//                }
//            }
//            return createCacheByMutex(id);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            mutexLock.releaseLock(LOCK_SHOP_KEY + id);
//        }

        //2.逻辑过期(在建立缓存时不在添加ttl,而是使用一个字段代表过期时间) + 互斥锁 + 额外线程解决
        //2.1 尝试获取锁,如果获取锁,则创建一个线程,用于缓存的重建

        //2.2 如果获取锁失败,直接返回旧数据

        return null;
    }

    /**
     * 获取缓存中的数据(对应逻辑过期 + 互斥锁)
     * @param id
     * @return null 数据不存在 Shop 缓存中的数据
     * @Versoin 1.0
     */
    private Shop getCacheData(Long id){
        String cacheShop = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        //缓存中不存在数据或为空数据
        if (!StringUtils.hasText(cacheShop)) {
            //避免缓存穿透//空数据代表缓存和数据库中均没有
            if("".equals(cacheShop)){
                return new Shop();
            }
            //返回null代表缓存中没有,要去数据库查找,如果数据库也没有,就存储一个空数据
            return null;
        }
        Shop shop = JSON.parseObject(cacheShop, Shop.class);
        if(StringUtils.hasText(shop.getExpire()) && Long.valueOf(shop.getExpire()) > System.currentTimeMillis()){
            return JSON.parseObject(cacheShop, Shop.class);
        }
        return null;
    }
    /**
     * 重新建立缓存,基于互斥锁
     * @param id
     * @return
     * @throws InterruptedException
     */
    private Shop createCacheByMutex(Long id) throws InterruptedException {
        //增大缓存重建难度
        Thread.sleep(2000);
        //查询失败从数据库查询
        Shop shop = shopMapper.selectById(id);
        //数据库查询失败返回错误
        if (shop == null) {
            //此处使用保存空对象来避免缓存穿透
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", Duration.ofMinutes(CACHE_NULL_TTL + RandomUtil.randomInt(0, 5)));
            return null;
        }
        //数据库查询成功保存至redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSON.toJSONString(shop), Duration.ofMinutes(CACHE_SHOP_TTL + RandomUtil.randomInt(0, 5)));
        //返回店铺数据
        return shop;
    }

    /**
     * 重新建立缓存,基于逻辑过期 + 互斥锁
     * @return
     */
    private Shop createCacheByLogicExpire(Long id){
        return null;
    }
    /**
     * 更新店铺信息
     * 需要考虑数据库与redis数据的一致性以及在高并发时出现读写数据库缓存数据问题
     * 更新数据库 -> 删除redis缓存数据(也可以直接更新redis缓存 , 但是如果对于一段时间内多次修改数据库的操作就需要多次更新缓存,存在多余操作)
     * 1. 数据库 -> redis
     * 2. redis -> 数据库
     * 两种操作均会出现高并发时缓存与数据库数据不一致的情况,第一种相对于第二种好一些
     *
     * @param shop
     */
    @Override
    @Transactional
    public void UpdateShopInfo(Shop shop) {
        if (shop.getId() == null) {
            throw new RuntimeException("店铺信息更新失败->店铺ID为空");
        }
        shopMapper.updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId().toString());
    }

}
