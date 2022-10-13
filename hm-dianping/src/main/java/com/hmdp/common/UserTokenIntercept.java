package com.hmdp.common;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.ConcreteUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 14:34
 * @Version 1.0
 * @Description 登录效验,检查用户是否登录
 */
@Slf4j
public class UserTokenIntercept implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("用户凭证拦截器 - 请求URI:{}", request.getRequestURI());
        UserDTO userDTO = ConcreteUser.get();
        return userDTO != null;
    }
}
