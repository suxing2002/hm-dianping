package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    /**
     * 优惠劵秒杀.涉及高并发,为避免线程危机
     * 1.乐观锁 2.将公共资源放在redis 3.修改mysql默认隔离级别
     * @param voucherId
     * @return
     */
    Result SeckillVoucher(Long voucherId);

    /**
     * 减少优惠券库存,并保存订单
     * @param voucherOrder
     */
    void insertOrder(VoucherOrder voucherOrder);
}
