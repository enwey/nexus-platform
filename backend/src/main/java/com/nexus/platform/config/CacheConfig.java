package com.nexus.platform.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheManager<String, Object> cacheManager = new RedisCacheManager<>();
        cacheManager.setConnectionFactory(connectionFactory);
        cacheManager.setDefaultExpiration(3600); // 1 hour
        cacheManager.setCachePrefix("nexus:");
        cacheManager.setKeySerializer(new StringRedisSerializer());
        cacheManager.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return cacheManager;
    }

    @Bean
    public RedisCacheWriter<Object> redisCacheWriter(RedisConnectionFactory connectionFactory) {
        return new RedisCacheWriter<>(connectionFactory);
    }
}
