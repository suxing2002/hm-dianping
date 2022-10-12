package com.hmdp;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.utils.SendCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class HmDianPingApplicationTests {
    @Autowired
    private SendCodeUtils sendCodeUtils;
    @Autowired
    private UserMapper userMapper;
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
}
