package com.hmdp.service;

import com.hmdp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {
    /**
     * 获取shopTypeList
     * 基于redis + mysql
     * @return
     */
    List<ShopType> getShopTypeForList();
}
