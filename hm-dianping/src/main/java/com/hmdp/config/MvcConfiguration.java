package com.hmdp.config;

import com.hmdp.common.SignInIntercept;
import org.springframework.boot.SpringBootConfiguration;
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
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        SignInIntercept signInIntercept = new SignInIntercept();
        InterceptorRegistration registration = registry.addInterceptor(signInIntercept);
//        registration.addPathPatterns("/**");
        registration.excludePathPatterns(
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
