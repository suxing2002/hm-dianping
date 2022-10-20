package com.hmdp;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;


import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_LOGIC_EXPIRE;

@SpringBootTest
@Slf4j
class HmDianPingApplicationTests {
    @Autowired
    private SendCodeUtils sendCodeUtils;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private MutexLock mutexLock;
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ShopMapper shopMapper;
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;
    @Resource
    private GlobalIDGenerator generator;
    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    private ExecutorService threadPool = Executors.newFixedThreadPool(500);
   @Autowired
   private SeckillVoucherMapper seckillVoucherMapper;
    @Test
    void test() {
        seckillVoucherMapper.selectById(14);
    }

    @DisplayName("测试验证码发送")
    @Test
    void testSendCode() {
        String code = RandomUtil.randomNumbers(6);
        sendCodeUtils.SendCode("18266033511", code);
    }

    /**
     * ew.sqlSegment != null and ew.sqlSegment != '' and ew.nonEmptyOfWhere
     * lombok不支持java18(忘记更换1.8),引起mp报错
     */
    @Test
    void testMapper() {
//        User user = userMapper.selectById(1);
//        log.info("user:{}",user);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, "18266033511");
        User user = userMapper.selectOne(queryWrapper);
//        List<User> users = userMapper.selectList(null);
//        users.forEach(System.out::println);
        System.out.println(user);
    }

    @DisplayName("测试Spring原生BeanUtils")
    @Test
    public void testBeanUtils() {
        User user = new User();
//        user.setPhone("18266033511");
        user.setId(1L);
        user.setNickName("无敌的承太郎");
        Object userObj = (Object) user;
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userObj, userDTO);
        System.out.println(userDTO.getNickName());
        System.out.println(userDTO.getId());
    }

    @Test
    void name() {
        CopyOptions copyOptions = CopyOptions.create().setIgnoreError(false).setFieldValueEditor((fieldName, fieldKey) -> fieldKey.toString());
        System.out.println(copyOptions.getClass());
    }

    @Test
    void test01() {
        //数据的类型依据运行时类型
        String str = "123";
        Object obj = str;
        System.out.println(obj.getClass());
    }

    @Test
    @DisplayName("测试自定义互斥锁")
    void test02() {
        boolean lock = mutexLock.getLock(CACHE_SHOP_KEY + 1);
        mutexLock.releaseLock(CACHE_SHOP_KEY + 1);
        log.info("{}", lock);
        boolean lock1 = mutexLock.getLock(CACHE_SHOP_KEY + 1);
        log.info("{}", lock1);
    }

    @Test
    @DisplayName("缓存预热")
    void cacheBeforeFire() {
        Shop queryResult = shopMapper.selectById(1);
        RedisData cache = new RedisData(LocalDateTime.now().plusSeconds(CACHE_SHOP_LOGIC_EXPIRE), queryResult);
        redisTemplate.opsForValue().set(CACHE_SHOP_KEY + 1, JSON.toJSONString(cache));
    }

    @DisplayName("测试全局Id生成器")
    @Test
    void testGlobalIDGenerator() throws InterruptedException {
        CountDownLatch downLatch = new CountDownLatch(400);
        long begin = System.currentTimeMillis();
        Runnable runnable = () -> {
            for (int i = 0; i < 100; i++) {
                Long shopId = generator.getId("Shop");
                System.out.println(shopId);
            }
            downLatch.countDown();
        };
        for (int i = 0; i < 400; i++) {
            threadPool.submit(runnable);
        }
        downLatch.await();
        long end = System.currentTimeMillis();
        System.out.println(end - begin);
    }

    @Test
    void getVoucherJson() {
        //1	1	50元代金券	周一至周日均可使用	全场通用\n无需预约\n可无限叠加\不兑现、不找零\n仅限堂食
        //4750	5000	0	1	2022-01-04 09:42:39	2022-01-04 09:43:31

        Voucher voucher = new Voucher(
                2L,1L,"100元代金券","周一至周五均可使用" , "全场通用\\n无需预约\\n可无限叠加\\不兑现、不找零\\n仅限堂食",8000L,20000L,1,1);
        voucher.setStock(100);
        voucher.setBeginTime(LocalDateTime.now().plusDays(1));
        voucher.setEndTime(LocalDateTime.now().plusDays(6));
        System.out.println(JSON.toJSONString(voucher));
//        voucherService.save(voucher);
        // 保存秒杀信息
//        SeckillVoucher seckillVoucher = new SeckillVoucher();
//        seckillVoucher.setVoucherId(voucher.getId());
//        seckillVoucher.setStock(voucher.getStock());
//        seckillVoucher.setBeginTime(voucher.getBeginTime());
//        seckillVoucher.setEndTime(voucher.getEndTime());
//        seckillVoucherService.save(seckillVoucher);
    }
}
