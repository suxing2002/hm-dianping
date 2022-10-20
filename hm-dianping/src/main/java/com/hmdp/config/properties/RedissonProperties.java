package com.hmdp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @Author GuoShuo
 * @Time 2022/10/18 19:08
 * @Version 1.0
 * @Description
 */
@ConfigurationProperties(prefix = "redisson")
@Data
public class RedissonProperties {
    private String address;
    private String password;
}
