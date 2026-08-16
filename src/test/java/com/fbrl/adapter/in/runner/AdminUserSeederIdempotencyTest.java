package com.fbrl.adapter.in.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fbrl.application.port.out.LoadAdminUserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "admin.initial.username=seeder-idempotency-admin",
      "admin.initial.password=seeder-idempotency-password"
    })
@DisplayName("AdminUserSeeder idempotency 테스트")
class AdminUserSeederIdempotencyTest {

  @Autowired private AdminUserSeeder adminUserSeeder;
  @Autowired private LoadAdminUserPort loadAdminUserPort;

  @Test
  @DisplayName(
      "컨텍스트 기동 시 이미 관리자 계정이 생성된 상태에서 시더를 다시 실행해도 " + "DuplicateAdminUsernameException 없이 정상 스킵된다.")
  void run_whenAdminAlreadyExists_skipsWithoutException() {
    // 컨텍스트 기동 시 ApplicationRunner로 이미 한 번 실행되어 계정이 생성된 상태여야 함.
    assertThat(loadAdminUserPort.loadByUsername("seeder-idempotency-admin")).isPresent();

    // "컨텍스트 재로드"를 흉내: 이미 계정이 있는 상태에서 시더를 한 번 더 실행.
    assertThatCode(() -> adminUserSeeder.run(new DefaultApplicationArguments()))
        .doesNotThrowAnyException();
  }
}
