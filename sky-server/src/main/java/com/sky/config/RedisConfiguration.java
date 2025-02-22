package com.sky.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate配置类，可以不写
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("初始化redisTemplate...");
        // set the serializer, default for JdkSerializationRedisSerializer
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // !! Notice: (not recommend) it is without type transformation automatically if add the serializer to value
        // redisTemplate.setValueSerializer(new StringRedisSerializer());
        // create the object through factory
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;

    };
}
