package com.hmdp.common;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.ConcreteUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @Author GuoShuo
 * @Time 2022/10/13 19:33
 * @Version 1.0
 * @Description 全局拦截器,负责来自不需要登录信息页面请求的拦截 , 其主要功能并非拦截 , 而是将用户数据的查询-保存与验证分离
 */
@Order(Integer.MIN_VALUE)
@Slf4j
public class GlobalIntercept implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public GlobalIntercept(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    /**
     * @Description 对所有请求进行拦截,如果用户已登录,将数据存储至ThreadLocal,并刷新token的保存时间
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler chosen handler to execute, for type and/or instance evaluation
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        log.info("全局拦截器 - 请求URI:{}", request.getRequestURI());
        String token = request.getHeader("authorization");
        if(!StringUtils.hasText(token)){
            return true;
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        if (entries.size() == 0) {
            return true;
        }
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofSeconds(LOGIN_USER_TTL));
        UserDTO userDTO = BeanUtil.mapToBean(entries, UserDTO.class, true, null);
        if (userDTO == null) {
            return true;
        }
        ConcreteUser.set(userDTO);
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ConcreteUser.clear();
    }
}

