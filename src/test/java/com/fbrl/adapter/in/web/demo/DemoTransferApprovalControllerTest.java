package com.fbrl.adapter.in.web.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.in.web.dto.RequestTransferApprovalRequest;
import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.adapter.out.persistence.ApprovalPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoAccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoApprovalRequestPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoEodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.demo.DemoLedgerEntryPersistenceAdapter;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.application.service.DemoAccountBalanceCalculator;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.domain.model.ExecutionStatus;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
@DisplayName("DemoTransferApprovalController 통합 테스트")
class DemoTransferApprovalControllerTest {

  private static final String MAKER_USERNAME = "demo-approval-test-maker";
  private static final String CHECKER_USERNAME = "demo-approval-test-checker";
  private static final String PASSWORD = "correct-password-123";
  private static final String SENDER = "DEMO-APPROVAL-111";
  private static final String RECEIVER = "DEMO-APPROVAL-222";
  private static final String TRIGGER_URL = "/api/v1/demo/transfer-approvals";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private DemoApprovalRequestPersistenceAdapter demoApprovalRequestPersistenceAdapter;
  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  @Autowired private DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  @Autowired private DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;
  @Autowired private DemoAccountBalanceCalculator demoAccountBalanceCalculator;

  @Autowired
  @Qualifier("demo")
  private SaveLedgerEntryPort demoSaveLedgerEntryPort;

  @Autowired private ApprovalPersistenceAdapter approvalPersistenceAdapter;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private LoadApprovalRequestPort loadApprovalRequestPort;
  @Autowired private LoadLedgerEntriesPort loadLedgerEntriesPort;

  private String makerToken;
  private String checkerToken;

  @BeforeEach
  void setUp() throws Exception {
    demoApprovalRequestPersistenceAdapter.deleteAllInBatch();
    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();

    approvalPersistenceAdapter.deleteAllInBatch();
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();

    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(MAKER_USERNAME, passwordEncoder.encode(PASSWORD)));
    saveAdminUserPort.save(AdminUser.create(CHECKER_USERNAME, passwordEncoder.encode(PASSWORD)));
    makerToken = login(MAKER_USERNAME);
    checkerToken = login(CHECKER_USERNAME);

    demoAccountPersistenceAdapter.save(Account.create(SENDER));
    demoAccountPersistenceAdapter.save(Account.create(RECEIVER));
    demoSaveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER,
                LedgerDirection.CREDIT,
                Money.wons(100_000_000),
                "TEST_SEED",
                Instant.now())));
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

  private String requestApproval(String token, BigDecimal amount) throws Exception {
    RequestTransferApprovalRequest requestBody =
        new RequestTransferApprovalRequest(SENDER, RECEIVER, amount);
    String responseBody =
        mockMvc
            .perform(
                post(TRIGGER_URL)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(responseBody).get("requestId").asText();
  }

  @Test
  @DisplayName("토큰 없이 기안을 시도하면 401을 반환한다.")
  void requestApproval_withoutToken_returns401() throws Exception {
    RequestTransferApprovalRequest requestBody =
        new RequestTransferApprovalRequest(SENDER, RECEIVER, BigDecimal.valueOf(20_000_000));

    mockMvc
        .perform(
            post(TRIGGER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName(
      "기안자 본인의 로그인 세션으로 자신이 기안한 데모 요청을 승인하면 SelfApprovalNotAllowedException이 터져 400을 반환한다"
          + "(도메인 로직 공유 확인).")
  void approveBySameLoginSession_isRejectedAsSelfApproval() throws Exception {
    String requestId = requestApproval(makerToken, BigDecimal.valueOf(20_000_000));

    mockMvc
        .perform(
            post(TRIGGER_URL + "/" + requestId + "/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer(makerToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SELF_APPROVAL_NOT_ALLOWED"));
  }

  @Test
  @DisplayName("서로 다른 두 계정으로 정상 승인하면 APPROVED 상태와 함께 실제 데모 이체까지 실행된다.")
  void approveByDifferentAdmin_succeedsAndExecutesDemoTransfer() throws Exception {
    String requestId = requestApproval(makerToken, BigDecimal.valueOf(20_000_000));

    mockMvc
        .perform(
            post(TRIGGER_URL + "/" + requestId + "/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer(checkerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));

    TransferApprovalRequest persisted =
        demoApprovalRequestPersistenceAdapter.loadByRequestId(requestId).orElseThrow();
    assertThat(persisted.getCheckerId()).isEqualTo(CHECKER_USERNAME);
    assertThat(persisted.getExecutionStatus()).isEqualTo(ExecutionStatus.EXECUTED);

    assertThat(demoAccountBalanceCalculator.calculate(Account.create(SENDER)))
        .isEqualTo(Money.wons(80_000_000));
    assertThat(demoAccountBalanceCalculator.calculate(Account.create(RECEIVER)))
        .isEqualTo(Money.wons(20_000_000));
  }

  @Test
  @DisplayName(
      "이상거래 임계치(5천만원) 이상 금액을 승인하면 status는 APPROVED로 유지되고 executionStatus는 FAILED로 남는다"
          + "(과제 24 승인/집행 분리 로직 재사용 확인).")
  void approveAboveFraudThreshold_staysApprovedButExecutionFails() throws Exception {
    String requestId = requestApproval(makerToken, BigDecimal.valueOf(60_000_000));

    mockMvc
        .perform(
            post(TRIGGER_URL + "/" + requestId + "/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer(checkerToken)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SUSPICIOUS_TRANSFER"));

    TransferApprovalRequest persisted =
        demoApprovalRequestPersistenceAdapter.loadByRequestId(requestId).orElseThrow();
    assertThat(persisted.getStatus().name()).isEqualTo("APPROVED");
    assertThat(persisted.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(persisted.getExecutionFailureReason()).contains("이상거래로 의심되어 이체가 차단되었습니다");

    assertThat(demoAccountBalanceCalculator.calculate(Account.create(SENDER)))
        .isEqualTo(Money.wons(100_000_000));
  }

  @Test
  @DisplayName("데모 승인 결과는 데모 DB에만 기록되고 운영 DB엔 전혀 없다.")
  void demoApproval_recordsOnlyInDemoDatabase() throws Exception {
    String requestId = requestApproval(makerToken, BigDecimal.valueOf(20_000_000));

    mockMvc
        .perform(
            post(TRIGGER_URL + "/" + requestId + "/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer(checkerToken)))
        .andExpect(status().isOk());

    assertThat(demoApprovalRequestPersistenceAdapter.loadByRequestId(requestId)).isPresent();

    assertThat(loadApprovalRequestPort.loadByRequestId(requestId)).isEmpty();
    assertThat(accountPersistenceAdapter.findByAccountNumber(SENDER)).isEmpty();
    assertThat(accountPersistenceAdapter.findByAccountNumber(RECEIVER)).isEmpty();
    assertThat(loadLedgerEntriesPort.loadByAccountNumberSince(SENDER, Instant.EPOCH)).isEmpty();
  }

  @Test
  @DisplayName("조회 API로 데모 승인 요청 상세를 확인할 수 있다.")
  void getApprovalRequest_returnsDetail() throws Exception {
    String requestId = requestApproval(makerToken, BigDecimal.valueOf(20_000_000));

    mockMvc
        .perform(
            get(TRIGGER_URL + "/" + requestId)
                .header(HttpHeaders.AUTHORIZATION, bearer(checkerToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value(requestId))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }
}
