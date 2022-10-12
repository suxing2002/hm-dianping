package com.hmdp.service.impl;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SendCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private SendCodeUtils sendCodeUtils;
    @Autowired
    private UserMapper userMapper;
    /**
     * 发送短信 效验号码格式 生成验证码 发送验证码 存储验证码
     * 基于session(无法使验证码定时失效)
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCodeWithSession(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号码格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
//        sendCodeUtils.SendCode(phone,code);
        log.info("发送验证码成功: {} -> {}", code , phone);
        session.setAttribute(phone, code);
        return Result.ok();
    }

    /**
     * 根据 phone - code 或 phone - password 登录 (只做验证码)
     * 基于session
     * phone - code : 验证码验证 查询用户 存储用户
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result userSignInBySession(LoginFormDTO loginForm, HttpSession session) {
        String code = loginForm.getCode();
        String phone = loginForm.getPhone();
        if(!RegexUtils.isPhoneInvalid(phone) && StringUtils.hasText(code)){
            if(code.equals(session.getAttribute(phone))){
                //验证码正确
                LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(User::getPhone, phone);
                User user = userMapper.selectOne(queryWrapper);
                if(user == null){
                    //基于session设置公共字段还要使用ThreadLocal存储session,太麻烦
                    //以后更换redis,这里手动填充
                   user = SaveUserByPhone(phone);
                }
                session.setAttribute("user", user);
                return Result.ok(user);
            }
        }
        return Result.fail("登陆失败,请检查号码或验证码是否正确");
    }

    private User SaveUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}
