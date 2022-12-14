package com.hmdp.mapper;

import com.hmdp.entity.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author 郭硕
 * @since 2022-01-04
 */
@Component
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

}
