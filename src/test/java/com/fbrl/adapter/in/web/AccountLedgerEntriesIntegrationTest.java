package com.fbrl.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
@DisplayName("AccountController 계좌별 원장 조회 통합 테스트")
class AccountLedgerEntriesIntegrationTest {

  private static final String USERNAME = "ledger-test-admin";
  private static final String PASSWORD = "correct-password-123";
  private static final String SENDER = "111-111";
  private static final String RECEIVER = "222-222";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;

  private String token;
  private Instant base;

  @BeforeEach
  void setUp() throws Exception {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();
    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(USERNAME, passwordEncoder.encode(PASSWORD)));
    token = login();

    accountPersistenceAdapter.save(Account.create(SENDER));
    accountPersistenceAdapter.save(Account.create(RECEIVER));
    base = Instant.parse("2026-08-01T00:00:00Z");
    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair("TEST-SEED-SOURCE", SENDER, Money.wons(1000), "TX-1", base)
            .entries());
    saveLedgerEntryPort.saveAll(
        LedgerEntry.transferPair(
                "TEST-SEED-SOURCE",
                SENDER,
                Money.wons(2000),
                "TX-2",
                base.plus(10, ChronoUnit.DAYS))
            .entries());
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
  @DisplayName("계좌번호와 기간으로 원장을 조회하면 범위 내 거래만 반환한다.")
  void getLedgerEntries_withinPeriod_returnsMatchingPage() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/accounts/" + SENDER + "/ledger-entries")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("from", base.toString())
                .param("to", base.plus(1, ChronoUnit.DAYS).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].accountNumber").value(SENDER))
        .andExpect(jsonPath("$.content[0].transactionId").value("TX-1"));
  }

  @Test
  @DisplayName("토큰 없이 조회를 시도하면 401을 반환한다.")
  void getLedgerEntries_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/accounts/" + SENDER + "/ledger-entries")
                .param("from", base.toString())
                .param("to", base.plus(20, ChronoUnit.DAYS).toString()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void getLedgerEntries_pageBeyondTotal_returnsEmptyContent() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/accounts/" + SENDER + "/ledger-entries")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("from", base.toString())
                .param("to", base.plus(20, ChronoUnit.DAYS).toString())
                .param("page", "5")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(2));
  }
}
