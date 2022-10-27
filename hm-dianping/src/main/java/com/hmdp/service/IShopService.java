package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {
    /**
     * 根据id获取店铺信息
     * 根据redis缓存获取数据
     * @param id
     * @return
     */
    Shop getShopDetailById(Long id);

    /**
     * 更新店铺信息
     * 需要考虑数据库与redis数据的一致性以及在高并发时出现读写数据库缓存数据问题
     *
     * @param shop
     */
    void UpdateShopInfo(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
