package com.fbrl.adapter.out.lock;

import com.fbrl.application.port.out.DemoResetStatusPort;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DemoResetStatusRedisAdapter implements DemoResetStatusPort {

  private static final String KEY = "demo:reset:last-completed-at";

  private final StringRedisTemplate redisTemplate;

  public DemoResetStatusRedisAdapter(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public void recordResetCompleted(Instant completedAt) {
    redisTemplate.opsForValue().set(KEY, completedAt.toString());
  }

  @Override
  public Optional<Instant> loadLastResetAt() {
    String value = redisTemplate.opsForValue().get(KEY);
    return Optional.ofNullable(value).map(Instant::parse);
  }
}
