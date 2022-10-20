package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.*;

import static com.hmdp.entity.VoucherOrder.PAY_REMAINING_SUM;
import static com.hmdp.utils.RedisConstants.LOCK_VOUCHER_TTL;
import static com.hmdp.utils.RedisConstants.VOUCHER;

/**
 * <p>
 * 服务实现类
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
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private  BlockingQueue<VoucherOrder> blockingQueue = new ArrayBlockingQueue<>(1024 * 1024);
    private final static ExecutorService voucherOrderHandler = Executors.newFixedThreadPool(1);
    private IVoucherOrderService proxy;

    private class orderHandlerTask implements Runnable {
        @Override
        public void run() {
            RLock lock = null;
            try {
                while (true) {
                    //弹出元素//如果队列中没有其它元素,线程阻塞,循环停止
                    VoucherOrder take = blockingQueue.take();
                    lock = redissonClient.getLock(RedisConstants.LOCK_SECKILL_VOUCHER_ORDER + take.getUserId());
                    if (!lock.tryLock()) {
                        log.error("error:orderHandler-->tryLock");
                        return;
                    }
                    proxy.insertOrder(take);
                }
            } catch (Exception e) {
                log.error("秒杀劵订单异常:", e);
            }finally {
                if(lock != null){
                    lock.unlock();
                }
            }
        }
    }

    @PostConstruct
    public void init() {
        voucherOrderHandler.submit(new orderHandlerTask());
    }
    private final static DefaultRedisScript<Long> secKillScript;

    static {
        secKillScript = new DefaultRedisScript<>();
        secKillScript.setLocation(new ClassPathResource("luaScript/SecKillVoucher.lua"));
        secKillScript.setResultType(Long.class);
    }

    /**
     * 优惠劵秒杀.涉及高并发,为避免线程危机
     * 1.乐观锁 2.将公共资源放在redis 3.使用特定方法(根据情况分析呗)
     * 1.乐观锁
     * 1.版本号:使用版本号判断数据是否已经被修改(通用)
     * 2.CAS (compare and swap) 通过比较要修改的字段,来判断数据是否被修改过,cas通常适用于数据的修改呈现单调性(否则会出现aba问题)
     * 对于优惠券秒杀,优惠券的stock只会减少,是不会出现aba问题的,更适用于cas
     * 乐观锁的缺点:由于是锁,所以存在大量线程修改失败的情况,并且是直接与数据库交互,容易引起数据库崩溃
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result SeckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("优惠券ID错误");
        }
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("优惠券尚未开始售卖");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("优惠券已结束售卖");
        }
        if (seckillVoucher.getStock() < 1) {
            return Result.fail("优惠券已售完");
        }
        //version 3.0
        //基于redis + 阻塞队列 + 线程池 实现异步,将下单与订单的创建分离
        Long userId = ConcreteUser.get().getId();
        Long result = redisTemplate.execute(secKillScript, Collections.emptyList(), voucherId.toString(), userId.toString());
        if (result != 0) {
            return Result.fail(getFailResult(result));
        }
        GlobalIDGenerator idGenerator = new GlobalIDGenerator(redisTemplate);
        Long orderId = idGenerator.getId(RedisConstants.SHOP_SECKILL_VOUCHER + voucherId);
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        //支付方式 1：余额；2：支付宝；3：微信
        voucherOrder.setPayType(PAY_REMAINING_SUM);
        //订单状态，1：未支付；2：已支付；3：已核销；4：已取消；5：退款中；6：已退款
        voucherOrder.setStatus(1);
        if(proxy == null){
            proxy = (IVoucherOrderService) AopContext.currentProxy();
        }
        blockingQueue.add(voucherOrder);
        return Result.ok(orderId);
    }


    private String getFailResult(Long result) {
        return result == 1 ? "优惠券库存不足" : "每人仅限购买一张";
    }
//    @Override
//    public Result SeckillVoucher(Long voucherId) {
//        SeckillVoucher seckillVoucher = seckillVoucherMapper.selectById(voucherId);
//        if (seckillVoucher == null) {
//            return Result.fail("优惠券ID错误");
//        }
//        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("优惠券尚未开始售卖");
//        }
//        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("优惠券已结束售卖");
//        }
//        if (seckillVoucher.getStock() < 1) {
//            return Result.fail("优惠券已售完");
//        }
//        //乐观锁:cas
////        LambdaQueryWrapper<SeckillVoucher> queryWrapper = new LambdaQueryWrapper<>();
////        queryWrapper.eq(SeckillVoucher::getStock , seckillVoucher.getStock())
////                .eq(SeckillVoucher::getVoucherId, seckillVoucher.getVoucherId());
////        seckillVoucher.setStock(seckillVoucher.getStock() - 1);
////        if(seckillVoucherMapper.update(seckillVoucher , queryWrapper) != 1){
////            return Result.fail("服务器繁忙,请稍后重试");
////        }
//        //特殊方式:
//        Long id = ConcreteUser.get().getId();
//        //synchronized关键字对某一对象添加锁,只有相同对象访问块才回被锁
////        synchronized (id.toString().intern()){
////        GlobalLock lock = new GlobalLock(redisTemplate, VOUCHER + id);
//        RLock lock = redissonClient.getLock("lock:voucher:" + id);
//        if (!lock.tryLock()) {
//            return Result.fail("服务器繁忙,请稍后重试");
//        }
//        //事务回滚是通过代理做到的,被代理对象方法之间的调用会导致代理方法失效(调用的是被代理对象的方法),需要调用的是代理对象的方法
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.insertOrder(seckillVoucher);
//        } finally {
//            lock.unlock();
//        }
////        }
//    }

    /**
     * 减少优惠券库存,并保存订单
     * @param order
     */
    @Transactional
    public void insertOrder(VoucherOrder order) {
        boolean update = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", order.getVoucherId())
                .ge("stock", 1).update();
        if(!update){
            throw new RuntimeException("修改优惠券库存失败:" + order.getVoucherId());
        }
        boolean save = save(order);
        if(!save){
            throw new RuntimeException("保存订单失败:" + order.getId());
        }
    }
}
