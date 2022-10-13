package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @Author GuoShuo
 * @Time 2022/10/12 20:54
 * @Version 1.0
 * @Description
 */
public class CustomerBeanUtils {
    private CustomerBeanUtils() {
    }

    /**
     * 将map中的value全部转换为String
     *
     * @param
     * @return
     */
    public static Map<String, Object> beanToMapString(Object bean) {
        HashMap<String, Object> resultMap = new HashMap<>();
        CopyOptions copyOptions = CopyOptions.create().setIgnoreError(false).
                setFieldValueEditor((fieldName, fieldKey) -> fieldKey.toString());
        BeanUtil.beanToMap(bean, resultMap, copyOptions);
        return resultMap;
    }

}
