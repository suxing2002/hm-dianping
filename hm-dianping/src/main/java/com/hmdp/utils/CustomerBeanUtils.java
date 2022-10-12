package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 20:54
 * @Version 1.0
 * @Description
 */
public class CustomerBeanUtils {
    private CustomerBeanUtils(){}
    public static Map<String , String> beanToMapString(Object bean){
        Map<String, Object> objectMap = BeanUtil.beanToMap(bean);
        HashMap<String, String> resultMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
            resultMap.put(entry.getKey(), entry.getValue().toString());
        }
        return resultMap;
    }
}
