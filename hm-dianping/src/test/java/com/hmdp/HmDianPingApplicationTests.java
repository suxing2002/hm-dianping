package com.hmdp;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.*;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;


import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    @Resource(name = "blogServiceImpl")
    private IBlogService blogServiceImpl;
    @Resource(name = "shopServiceImpl")
    private IShopService shopService;
    private ExecutorService threadPool = Executors.newFixedThreadPool(500);
   @Autowired
   private SeckillVoucherMapper seckillVoucherMapper;
    @Test
    void test() {
        seckillVoucherMapper.selectById(14);
    }

    @DisplayName("?????????????????????")
    @Test
    void testSendCode() {
        String code = RandomUtil.randomNumbers(6);
        sendCodeUtils.SendCode("18266033511", code);
    }

    /**
     * ew.sqlSegment != null and ew.sqlSegment != '' and ew.nonEmptyOfWhere
     * lombok?????????java18(????????????1.8),??????mp??????
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

    @DisplayName("??????Spring??????BeanUtils")
    @Test
    public void testBeanUtils() {
        User user = new User();
//        user.setPhone("18266033511");
        user.setId(1L);
        user.setNickName("??????????????????");
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
        //????????????????????????????????????
        String str = "123";
        Object obj = str;
        System.out.println(obj.getClass());
    }

    @Test
    @DisplayName("????????????????????????")
    void test02() {
        boolean lock = mutexLock.getLock(CACHE_SHOP_KEY + 1);
        mutexLock.releaseLock(CACHE_SHOP_KEY + 1);
        log.info("{}", lock);
        boolean lock1 = mutexLock.getLock(CACHE_SHOP_KEY + 1);
        log.info("{}", lock1);
    }

    @Test
    @DisplayName("????????????")
    void cacheBeforeFire() {
        Shop queryResult = shopMapper.selectById(1);
        RedisData cache = new RedisData(LocalDateTime.now().plusSeconds(CACHE_SHOP_LOGIC_EXPIRE), queryResult);
        redisTemplate.opsForValue().set(CACHE_SHOP_KEY + 1, JSON.toJSONString(cache));
    }

    @DisplayName("????????????Id?????????")
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
        //1	1	50????????????	???????????????????????????	????????????\n????????????\n???????????????\?????????????????????\n????????????
        //4750	5000	0	1	2022-01-04 09:42:39	2022-01-04 09:43:31

        Voucher voucher = new Voucher(
                2L,1L,"100????????????","???????????????????????????" , "????????????\\n????????????\\n???????????????\\?????????????????????\\n????????????",8000L,20000L,1,1);
        voucher.setStock(100);
        voucher.setBeginTime(LocalDateTime.now().plusDays(1));
        voucher.setEndTime(LocalDateTime.now().plusDays(6));
        System.out.println(JSON.toJSONString(voucher));
//        voucherService.save(voucher);
        // ??????????????????
//        SeckillVoucher seckillVoucher = new SeckillVoucher();
//        seckillVoucher.setVoucherId(voucher.getId());
//        seckillVoucher.setStock(voucher.getStock());
//        seckillVoucher.setBeginTime(voucher.getBeginTime());
//        seckillVoucher.setEndTime(voucher.getEndTime());
//        seckillVoucherService.save(seckillVoucher);
    }

    @Test
    void testMPQueryMethod() {
        Blog entity = blogServiceImpl.query().eq("id", 4).one();
        log.info(entity.toString());
    }

    @Test
    void testStringJoin() {
        HashSet<String> set = new HashSet<>();
        set.add("5");
        set.add("2");
        set.add("1");
        set.add("3");
        set.add("7");
        String join = String.join(",", set);
        log.info(join);
    }
    @Test
    void testStrUtilJoin() {
        HashSet<String> set = new HashSet<>();
        set.add("5");
        set.add("2");
        set.add("1");
        set.add("3");
        set.add("7");
        String join1 = StrUtil.join(",", set);
        log.info(join1);
    }

    @Test
    void testDoubleToLong() {
        Double aDouble = new Double(56.9);
        //?????????,???????????????(??????),??????????????????
        long result = aDouble.longValue();
        log.info(String.valueOf(result));
    }
    @DisplayName("????????????????????????redis")
    @Test
    void includeShopCoordinate() {
//        String GeoShopKey = RedisConstants.GEO_SHOP_KEY + type;
        List<Shop> shops = shopService.query().select("id" , "type_id" , "x" , "y").list();
        Map<Long, List<Shop>> shopMap = shops.stream().collect(Collectors.groupingBy(i -> i.getTypeId()));
        shopMap.forEach(
                (type , shopList) -> {
                    ArrayList<RedisGeoCommands.GeoLocation<String>> geoLocations = new ArrayList<>(shopList.size());
                    shopList.forEach(i ->
                            geoLocations.add(new RedisGeoCommands.GeoLocation<String>(String.valueOf(i.getId()) ,
                                    new Point(i.getX(), i.getY()))));
                    redisTemplate.opsForGeo().add(RedisConstants.GEO_SHOP_KEY + type , geoLocations);
                }
        );
    }

    @Test
    void testSteamMinMax() {
        int[] arr = {1 , 3 , 5, 2, 6 , 8, 4};
        IntStream intStream = Arrays.stream(arr);
        log.info(String.valueOf(intStream.min().getAsInt()));
        //min max??????stream??????,??????????????????????????????
        log.info(String.valueOf(intStream.max().getAsInt()));
    }
}
