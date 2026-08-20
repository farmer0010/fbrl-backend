package com.fbrl.adapter.out.lock;

import com.fbrl.application.port.out.DemoResetLockPort;
import com.fbrl.global.config.ShedLockProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShedLockRedisStatusAdapter implements DemoResetLockPort {

  private static final String LOCK_NAME = "demoDataReset";
  private static final String KEY_PREFIX = "job-lock";

  private final StringRedisTemplate redisTemplate;
  private final ShedLockProperties shedLockProperties;

  public ShedLockRedisStatusAdapter(
      StringRedisTemplate redisTemplate, ShedLockProperties shedLockProperties) {
    this.redisTemplate = redisTemplate;
    this.shedLockProperties = shedLockProperties;
  }

  @Override
  public boolean isLocked() {
    String key = KEY_PREFIX + ":" + shedLockProperties.environment() + ":" + LOCK_NAME;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}
