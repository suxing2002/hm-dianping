package com.hmdp.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ShopTypeMapper shopTypeMapper;
    @Override
    public List<ShopType> getShopTypeForList() {
        List<String> cacheShopType = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        if (cacheShopType.size() > 0) {
            return getShopListForShop(cacheShopType);
        }
        LambdaQueryWrapper<ShopType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(ShopType::getSort);
        List<ShopType> shopTypes = shopTypeMapper.selectList(queryWrapper);
        if(shopTypes.size() == 0){
            throw new RuntimeException("获取shop-type列表失败");
        }
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, getShopListForString(shopTypes));
        stringRedisTemplate.expire(CACHE_SHOP_TYPE_KEY, Duration.ofMinutes(CACHE_SHOP_TYPE_TTL + RandomUtil.randomInt(0, 5)));
        return shopTypes;
    }

    /**
     * List<ShopType> -> List<String>
     * @param shopTypes
     * @return
     */
    private List<String> getShopListForString(List<ShopType> shopTypes) {
        List<String> result = shopTypes.stream()
                .map(i -> JSON.toJSONString(i))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * List<String> -> List<ShopType>
     * stream真tm香
     * @param shopTypeList
     * @return
     */
    private List<ShopType> getShopListForShop(List<String> shopTypeList) {
        List<ShopType> result = shopTypeList.stream()
                .map(i -> JSON.parseObject(i, ShopType.class))
                .collect(Collectors.toList());
        return result;
    }
}
