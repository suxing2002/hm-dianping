package com.hmdp.config;

import com.hmdp.config.properties.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @Author GuoShuo
 * @Time 2022/10/18 19:01
 * @Version 1.0
 * @Description
 */
@SpringBootConfiguration
@EnableConfigurationProperties(com.hmdp.config.properties.RedissonProperties.class)
public class RedissonConfiguration {
    @Autowired
    private RedissonProperties properties;
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress(properties.getAddress()).setPassword(properties.getPassword());
        return Redisson.create(config);
    }
}
