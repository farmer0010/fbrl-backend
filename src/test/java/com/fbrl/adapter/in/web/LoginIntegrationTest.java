package com.fbrl.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("로그인 및 JWT 인증 통합 테스트")
class LoginIntegrationTest {

  private static final String USERNAME = "integration-test-admin";
  private static final String PASSWORD = "correct-password-123";

  @Autowired private MockMvc mockMvc;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(USERNAME, passwordEncoder.encode(PASSWORD)));
  }

  @Test
  @DisplayName("올바른 아이디/비밀번호로 로그인하면 토큰을 발급받고, 그 토큰으로 보호된 API를 호출할 수 있다.")
  void login_success_thenTokenWorksOnProtectedEndpoint() throws Exception {
    String loginBody = "{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}";

    String responseBody =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode json = objectMapper.readTree(responseBody);
    String token = json.get("token").asText();

    mockMvc
        .perform(
            get("/api/v1/accounts/no-such-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("비밀번호가 틀리면 401을 반환한다.")
  void login_wrongPassword_returns401() throws Exception {
    String loginBody = "{\"username\":\"" + USERNAME + "\",\"password\":\"wrong-password\"}";

    mockMvc
        .perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  @DisplayName("존재하지 않는 아이디로 로그인하면 401을 반환한다.")
  void login_unknownUsername_returns401() throws Exception {
    String loginBody = "{\"username\":\"no-such-admin\",\"password\":\"" + PASSWORD + "\"}";

    mockMvc
        .perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("토큰 없이 보호된 API를 호출하면 거부된다.")
  void protectedEndpoint_withoutToken_isRejected() throws Exception {
    mockMvc.perform(get("/api/v1/accounts/no-such-account")).andExpect(status().isForbidden());
  }
}
