package com.hmdp;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.utils.MutexLock;
import com.hmdp.utils.SendCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
@Slf4j
class HmDianPingApplicationTests {
    @Autowired
    private SendCodeUtils sendCodeUtils;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private MutexLock mutexLock;
    @DisplayName("测试验证码发送")
    @Test
    void testSendCode() {
        String code = RandomUtil.randomNumbers(6);
        sendCodeUtils.SendCode("18266033511",code);
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
    void test02(){
        boolean lock = mutexLock.getLock(CACHE_SHOP_KEY + 1);
        mutexLock.releaseLock(CACHE_SHOP_KEY + 1);
        log.info("{}", lock);
        boolean lock1 = mutexLock.getLock(CACHE_SHOP_KEY + 1);
        log.info("{}", lock1);
    }
}
