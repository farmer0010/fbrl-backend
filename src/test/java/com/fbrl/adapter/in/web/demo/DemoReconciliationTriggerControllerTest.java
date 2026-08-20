package com.fbrl.adapter.in.web.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@DisplayName("DemoReconciliationTriggerController 통합 테스트 — 온디맨드 트리거 200/409")
class DemoReconciliationTriggerControllerTest {

  private static final String USERNAME = "demo-reconciliation-trigger-test-admin";
  private static final String PASSWORD = "correct-password-123";
  private static final String TRIGGER_URL = "/api/v1/demo/batch-jobs/reconciliation/trigger";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired
  @Qualifier("demoJobRepository")
  private JobRepository demoJobRepository;

  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;

  @Autowired
  private DemoReconciliationDiscrepancyPersistenceAdapter
      demoReconciliationDiscrepancyPersistenceAdapter;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    new JobRepositoryTestUtils(demoJobRepository).removeJobExecutions();

    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoReconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();

    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(USERNAME, passwordEncoder.encode(PASSWORD)));
    token = login();
  }

  private String login() throws Exception {
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
    return json.get("token").asText();
  }

  private String bearer() {
    return "Bearer " + token;
  }

  @Test
  @DisplayName("첫 호출은 200과 함께 완료된 실행 정보를 반환한다.")
  void triggerReconciliation_firstCall_returns200() throws Exception {
    mockMvc
        .perform(post(TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.jobExecutionId").isNumber())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  @DisplayName("같은 날 두 번째 호출은 409와 함께 재실행 방지 메시지를 반환한다.")
  void triggerReconciliation_secondCallSameDay_returns409() throws Exception {
    mockMvc
        .perform(post(TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk());

    mockMvc
        .perform(post(TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("RECONCILIATION_ALREADY_SETTLED_TODAY"))
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이미 완료")));
  }

  @Test
  @DisplayName("토큰 없이 트리거를 시도하면 401을 반환한다.")
  void triggerReconciliation_withoutToken_returns401() throws Exception {
    mockMvc.perform(post(TRIGGER_URL)).andExpect(status().isUnauthorized());
  }
}
