package com.fbrl.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("RedissonConfig 설정 반영 테스트")
class RedissonConfigTest {

  @Nested
  @DisplayName("로컬 개발 기본값(비밀번호 없음, TLS 비활성)")
  class LocalDefaults {
    @Test
    @DisplayName("redis:// 프로토콜을 사용하고 비밀번호를 설정하지 않는다")
    void usesPlainRedisProtocolWithoutPassword() {
      RedissonConfig redissonConfig = new RedissonConfig();
      ReflectionTestUtils.setField(redissonConfig, "host", "localhost");
      ReflectionTestUtils.setField(redissonConfig, "port", 6379);
      ReflectionTestUtils.setField(redissonConfig, "password", "");
      ReflectionTestUtils.setField(redissonConfig, "sslEnabled", false);

      Config config = redissonConfig.buildConfig();

      var serverConfig = config.useSingleServer();
      assertThat(serverConfig.getAddress()).isEqualTo("redis://localhost:6379");
      assertThat(serverConfig.getPassword()).isNull();
    }
  }

  @Nested
  @DisplayName("Azure Cache for Redis 설정(비밀번호 있음, TLS 활성)")
  class AzureCacheForRedis {
    @Test
    @DisplayName("rediss:// 프로토콜을 사용하고 비밀번호를 설정한다")
    void usesTlsProtocolWithPassword() {
      RedissonConfig redissonConfig = new RedissonConfig();
      ReflectionTestUtils.setField(redissonConfig, "host", "my-cache.redis.cache.windows.net");
      ReflectionTestUtils.setField(redissonConfig, "port", 6380);
      ReflectionTestUtils.setField(redissonConfig, "password", "access-key");
      ReflectionTestUtils.setField(redissonConfig, "sslEnabled", true);

      Config config = redissonConfig.buildConfig();

      var serverConfig = config.useSingleServer();
      assertThat(serverConfig.getAddress())
          .isEqualTo("rediss://my-cache.redis.cache.windows.net:6380");
      assertThat(serverConfig.getPassword()).isEqualTo("access-key");
    }
  }
}
