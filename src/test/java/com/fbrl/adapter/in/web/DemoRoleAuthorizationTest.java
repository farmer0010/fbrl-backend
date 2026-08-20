package com.fbrl.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminRole;
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
@DisplayName("DEMO 역할 인가 통합 테스트 — /api/v1/demo/** 허용, 그 외 운영 엔드포인트 403")
class DemoRoleAuthorizationTest {

  private static final String DEMO_USERNAME = "role-test-demo-account";
  private static final String ADMIN_USERNAME = "role-test-admin-account";
  private static final String PASSWORD = "correct-password-123";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;

  private String demoToken;
  private String adminToken;

  @BeforeEach
  void setUp() throws Exception {
    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(
        AdminUser.create(DEMO_USERNAME, passwordEncoder.encode(PASSWORD), AdminRole.DEMO));
    saveAdminUserPort.save(
        AdminUser.create(ADMIN_USERNAME, passwordEncoder.encode(PASSWORD), AdminRole.ADMIN));
    demoToken = login(DEMO_USERNAME);
    adminToken = login(ADMIN_USERNAME);
  }

  private String login(String username) throws Exception {
    String loginBody = "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
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
    return json.get("token").asText();
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  @Test
  @DisplayName("로그인 응답에 역할(role)이 포함되고, DEMO/ADMIN 계정에 맞는 값이 내려온다.")
  void loginResponse_includesRole() throws Exception {
    String demoBody =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\""
                            + DEMO_USERNAME
                            + "\",\"password\":\""
                            + PASSWORD
                            + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("DEMO"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat(demoBody).contains("\"role\":\"DEMO\"");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  @DisplayName("DEMO 계정으로 로그인 후 /api/v1/demo/accounts(생성)는 성공(201)한다.")
  void demoAccount_canCreateDemoAccount() throws Exception {
    mockMvc
        .perform(post("/api/v1/demo/accounts").header(HttpHeaders.AUTHORIZATION, bearer(demoToken)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("DEMO 계정으로 로그인 후 운영 이체(/api/v1/transfers)를 시도하면 403을 반환한다.")
  void demoAccount_cannotAccessProductionTransfer() throws Exception {
    String body =
        "{\"senderAccountNumber\":\"111-111\",\"receiverAccountNumber\":\"222-222\",\"amount\":1000}";

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .header(HttpHeaders.AUTHORIZATION, bearer(demoToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName("DEMO 계정으로 로그인 후 운영 계좌 생성(/api/v1/accounts)을 시도하면 403을 반환한다.")
  void demoAccount_cannotAccessProductionAccountCreation() throws Exception {
    mockMvc
        .perform(post("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(demoToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName(
      "DEMO 계정으로 로그인 후 운영 배치 이력 조회(/api/v1/batch-jobs/{jobName}/executions)를 시도하면 403을 반환한다.")
  void demoAccount_cannotAccessProductionBatchJobHistory() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/batch-jobs/eodSettlementJob/executions")
                .header(HttpHeaders.AUTHORIZATION, bearer(demoToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName("ADMIN 계정은 데모 엔드포인트와 운영 엔드포인트 둘 다 여전히 접근 가능하다(회귀 없음).")
  void adminAccount_stillAccessesBothDemoAndProductionEndpoints() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/demo/accounts").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(post("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/v1/batch-jobs/eodSettlementJob/executions")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("토큰 없이 데모 엔드포인트를 호출하면 401을 반환한다.")
  void demoEndpoint_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(post("/api/v1/demo/accounts"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }
}
