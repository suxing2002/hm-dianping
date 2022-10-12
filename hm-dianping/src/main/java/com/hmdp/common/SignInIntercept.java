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
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 14:34
 * @Version 1.0
 * @Description 登录效验
 */
public class SignInIntercept implements HandlerInterceptor {
    /**
     * 在拦截器中是无法正常注入的,拦截器的加载时间在bean初始化之前,只能手动注入(也可以把拦截器作为bean加载,但是我没有成功)
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("authorization");
        if (stringRedisTemplate == null) {
            BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext());
            stringRedisTemplate = factory.getBean(StringRedisTemplate.class);
        }
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(token);
        User user = null;
        if (entries.size() != 0) {
            user = BeanUtil.mapToBean(entries, User.class, true, null);
        }
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        ConcreteUser.set(userDTO);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ConcreteUser.clear();
    }
}
