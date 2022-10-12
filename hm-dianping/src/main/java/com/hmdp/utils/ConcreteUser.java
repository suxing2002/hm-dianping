package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 14:36
 * @Version 1.0
 * @Description
 */
public class ConcreteUser {
    private static final ThreadLocal<UserDTO> userThreadLocal = new ThreadLocal<UserDTO>();
    public static void set(UserDTO user){
        userThreadLocal.set(user);
    }
    public static UserDTO get(){
        return userThreadLocal.get();
    }
    public static void clear(){
        userThreadLocal.remove();
    }
 }
