package com.fbrl.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
@DisplayName("spring.jpa.hibernate.ddl-auto가 데모 EntityManagerFactory에도 동일하게 적용되는지 검증")
class DemoEntityManagerFactoryDdlAutoTest {

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired
  @Qualifier("demoEntityManagerFactory")
  private EntityManagerFactory demoEntityManagerFactory;

  @Autowired private Environment environment;

  @Test
  @DisplayName("운영/데모 EntityManagerFactory 모두 spring.jpa.hibernate.ddl-auto 값을 그대로 물려받는다")
  void demoEntityManagerFactory_inheritsConfiguredDdlAuto() {
    String configuredDdlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
    assertThat(configuredDdlAuto).isEqualTo("update");

    assertThat(entityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto"))
        .isEqualTo(configuredDdlAuto);
    assertThat(demoEntityManagerFactory.getProperties().get("hibernate.hbm2ddl.auto"))
        .isEqualTo(configuredDdlAuto);
  }
}
