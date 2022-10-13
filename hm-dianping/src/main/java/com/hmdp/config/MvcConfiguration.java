package com.hmdp.config;

import com.hmdp.common.GlobalIntercept;
import com.hmdp.common.UserTokenIntercept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 14:32
 * @Version 1.0
 * @Description
 */
@SpringBootConfiguration
public class MvcConfiguration implements WebMvcConfigurer {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //全局拦截器
        InterceptorRegistration globalInterceptorRegistration = registry.addInterceptor(new GlobalIntercept(stringRedisTemplate));
        globalInterceptorRegistration.addPathPatterns("/**");
        //用户凭证拦截器
        InterceptorRegistration userTokenInterceptRegistration = registry.addInterceptor(new UserTokenIntercept());
        userTokenInterceptRegistration.addPathPatterns("/**");
        userTokenInterceptRegistration.excludePathPatterns(
                "/user/code" ,
                "/user/login",
                "/user/logout",
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/**"
        );
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
