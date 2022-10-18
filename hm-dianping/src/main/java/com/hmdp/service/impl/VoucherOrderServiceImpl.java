package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.ConcreteUser;
import com.hmdp.utils.GlobalIDGenerator;
import com.hmdp.utils.SystemConstants;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxy;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.beans.beancontext.BeanContextProxy;
import java.time.LocalDateTime;

import static com.hmdp.entity.VoucherOrder.PAY_REMAINING_SUM;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 郭硕
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private VoucherMapper voucherMapper;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private GlobalIDGenerator idGenerator;
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    /**
     * 优惠劵秒杀.涉及高并发,为避免线程危机
     * 1.乐观锁 2.将公共资源放在redis 3.使用特定方法(根据情况分析呗)
     * 1.乐观锁
     *      1.版本号:使用版本号判断数据是否已经被修改(通用)
     *      2.CAS (compare and swap) 通过比较要修改的字段,来判断数据是否被修改过,cas通常适用于数据的修改呈现单调性(否则会出现aba问题)
     *   对于优惠券秒杀,优惠券的stock只会减少,是不会出现aba问题的,更适用于cas
     * 乐观锁的缺点:由于是锁,所以存在大量线程修改失败的情况,并且是直接与数据库交互,容易引起数据库崩溃
     * @param voucherId
     * @return
     */
    @Override
    public Result SeckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
        if(seckillVoucher == null){
            return Result.fail("优惠券ID错误");
        }
        if(seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("优惠券尚未开始售卖");
        }
        if(seckillVoucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("优惠券已结束售卖");
        }
        if(seckillVoucher.getStock() < 1){
            return Result.fail("优惠券已售完");
        }
        //乐观锁:cas
//        LambdaQueryWrapper<SeckillVoucher> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(SeckillVoucher::getStock , seckillVoucher.getStock())
//                .eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId());
//        seckillVoucher.setStock(seckillVoucher.getStock() - 1);
//        if(seckillVoucherMapper.update(seckillVoucher , queryWrapper) != 1){
//            return Result.fail("服务器繁忙,请稍后重试");
//        }
        //特殊方式:
        Long id = ConcreteUser.get().getId();
        //synchronized关键字对某一对象添加锁,只有相同对象访问块才回被锁
        synchronized (id.toString().intern()){
            //事务回滚是通过代理做到的,被代理对象方法之间的调用会导致代理方法失效(调用的是被代理对象的方法),需要调用的是代理对象的方法
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.insertOrder(seckillVoucher);
        }
    }

    /**
     * 约束一人只能购买一张优惠券,通过锁 + 事务
     * @param seckillVoucher
     * @return
     */
    @Transactional
    public Result insertOrder(SeckillVoucher seckillVoucher) {
        Integer count = query().eq("user_id", ConcreteUser.get().getId())
                .eq("voucher_id", seckillVoucher.getVoucherId())
                .count();
        if(count != 0){
            return Result.fail("每人仅限购买一张");
        }
        boolean update = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", seckillVoucher.getVoucherId())
                .ge("stock", 1).update();
        if (!update) {
            return Result.fail("优惠券已售完");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(idGenerator.getId(SystemConstants.SHOP_SECKILL_VOUCHER));
        voucherOrder.setCreateTime(LocalDateTime.now());
        voucherOrder.setUpdateTime(LocalDateTime.now());
        voucherOrder.setUserId(ConcreteUser.get().getId());
        voucherOrder.setVoucherId(seckillVoucher.getVoucherId());
        //支付方式 1：余额；2：支付宝；3：微信
        voucherOrder.setPayType(PAY_REMAINING_SUM);
        //订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
        voucherOrder.setStatus(1);
        save(voucherOrder);
        return Result.ok(voucherOrder.getId());
    }
}
