package com.fbrl.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
  @Value("${spring.data.redis.host:localhost}")
  private String host;

  @Value("${spring.data.redis.port:6379}")
  private int port;

  @Value("${spring.data.redis.password:}")
  private String password;

  @Value("${spring.data.redis.ssl.enabled:false}")
  private boolean sslEnabled;

  private static final String REDIS_PROTOCOL = "redis://";
  private static final String REDISS_PROTOCOL = "rediss://";

  @Bean
  public RedissonClient redissonClient() {
    return Redisson.create(buildConfig());
  }

  Config buildConfig() {
    Config config = new Config();
    String protocol = sslEnabled ? REDISS_PROTOCOL : REDIS_PROTOCOL;
    var serverConfig = config.useSingleServer().setAddress(protocol + host + ":" + port);
    if (!password.isBlank()) {
      serverConfig.setPassword(password);
    }
    return config;
  }
}
