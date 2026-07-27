package com.fbrl;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("DB 설정이 완료되기 전까지 통합 테스트 비활성화")
class FbrlBackendApplicationTests {

  @Test
  void contextLoads() {}
}
