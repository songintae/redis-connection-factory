package com.example.redisconnectionfactory.repository;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class KeyValueRepository {
    private final StringRedisTemplate stringRedisTemplate;

    public KeyValueRepository(RedisConnectionFactory redisConnectionFactory) {
        this.stringRedisTemplate = new StringRedisTemplate(redisConnectionFactory);
    }

    public void save(String key , String value) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        valueOperations.set(key, value);
        stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    public Optional<String> findBy(String key) {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        return Optional.ofNullable(valueOperations.get(key));
    }
}
