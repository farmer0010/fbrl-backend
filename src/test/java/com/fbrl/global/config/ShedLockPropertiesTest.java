package com.fbrl.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("ShedLockProperties 바인딩 테스트")
class ShedLockPropertiesTest {

  @Nested
  @SpringBootTest
  @DisplayName("SHEDLOCK_ENVIRONMENT 미설정 시")
  class Default {
    @Autowired private ShedLockProperties shedLockProperties;

    @Test
    @DisplayName("기본값 fbrl-backend로 바인딩된다")
    void bindsDefaultValue() {
      assertThat(shedLockProperties.environment()).isEqualTo("fbrl-backend");
    }
  }

  @Nested
  @SpringBootTest(properties = "SHEDLOCK_ENVIRONMENT=demo")
  @DisplayName("SHEDLOCK_ENVIRONMENT 오버라이드 시")
  class Overridden {
    @Autowired private ShedLockProperties shedLockProperties;

    @Test
    @DisplayName("오버라이드 값으로 바인딩된다")
    void bindsOverriddenValue() {
      assertThat(shedLockProperties.environment()).isEqualTo("demo");
    }
  }
}
