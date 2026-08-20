package com.fbrl.adapter.in.web.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoApprovalRequestPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoOutboxPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.application.service.DemoDataResetService;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("데모 데이터 리셋 통합 테스트 — 테이블 초기화/재시딩, 변조 판정, 423 락, 배치 메타데이터 재트리거")
class DemoDataResetIntegrationTest {

  private static final String USERNAME = "demo-reset-test-admin";
  private static final String PASSWORD = "correct-password-123";
  private static final String EOD_TRIGGER_URL = "/api/v1/demo/batch-jobs/eod/trigger";
  private static final String RECONCILIATION_TRIGGER_URL =
      "/api/v1/demo/batch-jobs/reconciliation/trigger";
  private static final String RESET_LOCK_KEY = "job-lock:fbrl-backend:demoDataReset";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private DemoDataResetService demoDataResetService;
  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;

  @Autowired
  private DemoReconciliationDiscrepancyPersistenceAdapter
      demoReconciliationDiscrepancyPersistenceAdapter;

  @Autowired private DemoApprovalRequestPersistenceAdapter demoApprovalRequestPersistenceAdapter;
  @Autowired private DemoOutboxPersistenceAdapter demoOutboxPersistenceAdapter;
  @Autowired private StringRedisTemplate redisTemplate;

  @Autowired
  @Qualifier("demoJobRepository")
  private JobRepository demoJobRepository;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    new JobRepositoryTestUtils(demoJobRepository).removeJobExecutions();
    redisTemplate.delete(RESET_LOCK_KEY);

    demoOutboxPersistenceAdapter.deleteAllInBatch();
    demoApprovalRequestPersistenceAdapter.deleteAllInBatch();
    demoReconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();

    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(USERNAME, passwordEncoder.encode(PASSWORD)));
    token = login();
  }

  @AfterEach
  void tearDown() {
    redisTemplate.delete(RESET_LOCK_KEY);
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
  @DisplayName("리셋 실행 전 임의 데이터를 넣어도, 리셋 후에는 각 테이블이 정확히 초기 시드 상태로만 존재한다.")
  void reset_wipesJunkDataAndReseedsExactState() {
    demoAccountPersistenceAdapter.save(Account.create("JUNK-111"));
    demoAccountPersistenceAdapter.save(Account.create("JUNK-222"));

    demoDataResetService.reset();

    List<Account> accountsAfter = demoAccountPersistenceAdapter.loadAccounts(0, 100);
    assertThat(accountsAfter)
        .extracting(Account::getAccountNumber)
        .containsExactlyInAnyOrder(
            DemoDataResetService.SEED_SENDER_ACCOUNT_NUMBER,
            DemoDataResetService.SEED_RECEIVER_ACCOUNT_NUMBER);
    assertThat(accountsAfter).noneMatch(a -> a.getAccountNumber().startsWith("JUNK"));

    assertThat(demoApprovalRequestPersistenceAdapter.loadByStatus(null)).isEmpty();

    List<OutboxEvent> outboxEvents = demoOutboxPersistenceAdapter.loadAllOrderedById();
    assertThat(outboxEvents).hasSize(3);
  }

  @Test
  @DisplayName("리셋 후 마지막 outbox_event 1건만 entryHash 재계산이 어긋나고, 나머지는 정상 체인이다.")
  void reset_lastOutboxEventIsTamperedOnly() {
    demoDataResetService.reset();

    List<OutboxEvent> events = demoOutboxPersistenceAdapter.loadAllOrderedById();
    assertThat(events).hasSize(3);

    for (int i = 0; i < events.size() - 1; i++) {
      OutboxEvent event = events.get(i);
      assertThat(event.getEntryHash())
          .as("정상 이벤트(id=%s)는 재계산 해시와 일치해야 한다", event.getId())
          .isEqualTo(event.recomputeEntryHash());
    }

    OutboxEvent tamperedEvent = events.get(events.size() - 1);
    assertThat(tamperedEvent.getEntryHash())
        .as("마지막 이벤트(id=%s)는 payload가 변조되어 재계산 해시와 달라야 한다", tamperedEvent.getId())
        .isNotEqualTo(tamperedEvent.recomputeEntryHash());
    assertThat(tamperedEvent.getPayload()).contains("tampered");
  }

  @Test
  @DisplayName("demoDataReset 락이 보유 중이면 EOD/Reconciliation 온디맨드 트리거는 423을 반환한다.")
  void triggerEndpoints_return423WhenResetLockHeld() throws Exception {
    redisTemplate
        .opsForValue()
        .set(RESET_LOCK_KEY, "held-by-test", java.time.Duration.ofMinutes(1));

    mockMvc
        .perform(post(EOD_TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().is(423))
        .andExpect(jsonPath("$.code").value("DEMO_RESET_IN_PROGRESS"));

    mockMvc
        .perform(post(RECONCILIATION_TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().is(423))
        .andExpect(jsonPath("$.code").value("DEMO_RESET_IN_PROGRESS"));
  }

  @Test
  @DisplayName("락이 없으면 트리거는 평소대로 동작한다(회귀 없음).")
  void triggerEndpoints_workNormallyWithoutResetLock() throws Exception {
    mockMvc
        .perform(post(EOD_TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName(
      "당일 두 번째 EOD 트리거는 409로 막히지만, 리셋 후에는 배치 메타데이터가 데모 Job 한정으로 지워져 같은 날에도 다시 200으로 트리거된다.")
  void reset_clearsBatchMetadata_allowsRetriggerSameDay() throws Exception {
    mockMvc
        .perform(post(EOD_TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk());

    mockMvc
        .perform(post(EOD_TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EOD_ALREADY_SETTLED_TODAY"));

    demoDataResetService.reset();

    mockMvc
        .perform(post(EOD_TRIGGER_URL).header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("리셋 후 GET /api/v1/demo/reset-status는 방금 리셋된 시각과 다음 리셋 예정 시각을 함께 반환한다.")
  void resetStatus_reflectsLastResetAndComputesNextResetTime() throws Exception {
    demoDataResetService.reset();

    mockMvc
        .perform(get("/api/v1/demo/reset-status").header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lastResetAt").isNotEmpty())
        .andExpect(jsonPath("$.nextResetAt").isNotEmpty());
  }
}
