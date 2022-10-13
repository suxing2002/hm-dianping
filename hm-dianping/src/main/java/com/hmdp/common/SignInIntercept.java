package com.hmdp.common;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.utils.ConcreteUser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 14:34
 * @Version 1.0
 * @Description 登录效验
 */
public class SignInIntercept implements HandlerInterceptor {
    private StringRedisTemplate stringRedisTemplate;

    public SignInIntercept(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if(!StringUtils.hasText(token)){
            response.setStatus(401);
            return false;
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        if (entries.size() == 0) {
            response.setStatus(401);
            return false;
        }
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, Duration.ofSeconds(LOGIN_USER_TTL));
        UserDTO userDTO = BeanUtil.mapToBean(entries, UserDTO.class, true, null);
        if (userDTO == null) {
            response.setStatus(401);
            return false;
        }
        ConcreteUser.set(userDTO);
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ConcreteUser.clear();
    }
}
