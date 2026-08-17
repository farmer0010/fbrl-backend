package com.fbrl.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CORS 설정 통합 테스트")
class WebConfigCorsTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("설정된 오리진의 프리플라이트 요청은 허용된다.")
  void preflight_fromAllowedOrigin_isAllowed() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/accounts")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
  }

  @Test
  @DisplayName("설정되지 않은 오리진의 프리플라이트 요청은 거부된다.")
  void preflight_fromDisallowedOrigin_isRejected() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/accounts")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
  }
}
