package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {
    /**
     * 发送短信 效验号码格式 生成验证码 发送验证码 存储验证码
     *
     * @param phone
     * @param session
     * @return
     */
    Result sendCodeWithSession(String phone, HttpSession session);

    /**
     * 根据 phone - code 或 phone - password 登录
     * 基于session
     *
     * @param loginForm
     * @param session
     * @return
     */
    Result userSignInBySession(LoginFormDTO loginForm, HttpSession session);
}
