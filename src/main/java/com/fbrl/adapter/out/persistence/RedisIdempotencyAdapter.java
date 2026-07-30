package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.IdempotencyRepositoryPort;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisIdempotencyAdapter implements IdempotencyRepositoryPort {
  private StringRedisTemplate redisTemplate;

  public RedisIdempotencyAdapter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean setIfAbsent(String key, String value, Duration ttl) {
    Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
    return Boolean.TRUE.equals(result);
  }

  @Override
  public void setValue(String key, String value, Duration ttl) {
    redisTemplate.opsForValue().set(key, value, ttl);
  }

  @Override
  public String getValue(String key) {
    return redisTemplate.opsForValue().get(key);
  }
}
