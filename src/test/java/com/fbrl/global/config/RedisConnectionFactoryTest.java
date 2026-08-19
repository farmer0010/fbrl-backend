package com.fbrl.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@DisplayName("Boot 자동구성 RedisConnectionFactory(ShedLockConfig가 사용) 설정 반영 테스트")
class RedisConnectionFactoryTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class));

  @Nested
  @DisplayName("spring.data.redis.password/ssl.enabled 미설정 시")
  class Default {
    @Test
    @DisplayName("비밀번호가 없고 SSL이 비활성 상태로 구성된다")
    void configuresWithoutPasswordOrSsl() {
      contextRunner.run(
          context -> {
            LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
            assertThat(factory.getPassword()).isNullOrEmpty();
            assertThat(factory.isUseSsl()).isFalse();
          });
    }
  }

  @Nested
  @DisplayName("spring.data.redis.password/ssl.enabled 오버라이드 시")
  class Overridden {
    @Test
    @DisplayName("비밀번호와 SSL 설정이 그대로 반영된다")
    void configuresWithPasswordAndSsl() {
      contextRunner
          .withPropertyValues(
              "spring.data.redis.password=test-access-key", "spring.data.redis.ssl.enabled=true")
          .run(
              context -> {
                LettuceConnectionFactory factory = context.getBean(LettuceConnectionFactory.class);
                assertThat(factory.getPassword()).isEqualTo("test-access-key");
                assertThat(factory.isUseSsl()).isTrue();
              });
    }
  }
}
