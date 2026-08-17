package com.fbrl.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.domain.model.EodSnapshot;
import com.fbrl.domain.model.Money;
import java.time.LocalDate;
import java.util.List;
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
@DisplayName("AccountController 계좌별 EOD 스냅샷 이력 조회 통합 테스트")
class AccountEodSnapshotHistoryIntegrationTest {

  private static final String USERNAME = "eod-history-test-admin";
  private static final String PASSWORD = "correct-password-123";
  private static final String ACCOUNT = "111-111";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(USERNAME, passwordEncoder.encode(PASSWORD)));
    token = login();

    eodSnapshotPersistenceAdapter.saveAll(
        List.of(
            EodSnapshot.of(
                ACCOUNT, Money.wons(10_000_000), Money.wons(1000), LocalDate.of(2026, 8, 1)),
            EodSnapshot.of(
                ACCOUNT, Money.wons(11_000_000), Money.wons(1100), LocalDate.of(2026, 8, 10))));
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
  @DisplayName("from/to 없이 조회하면 해당 계좌의 전체 스냅샷을 반환한다.")
  void getEodSnapshotHistory_withoutRange_returnsAll() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/accounts/" + ACCOUNT + "/eod-snapshots")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @DisplayName("from/to로 조회하면 범위 내 스냅샷만 반환한다.")
  void getEodSnapshotHistory_withRange_filtersByDate() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/accounts/" + ACCOUNT + "/eod-snapshots")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("from", "2026-08-01")
                .param("to", "2026-08-05"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].settlementDate").value("2026-08-01"));
  }

  @Test
  @DisplayName("토큰 없이 조회를 시도하면 401을 반환한다.")
  void getEodSnapshotHistory_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/accounts/" + ACCOUNT + "/eod-snapshots"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void getEodSnapshotHistory_pageBeyondTotal_returnsEmptyContent() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/accounts/" + ACCOUNT + "/eod-snapshots")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("page", "5")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(2));
  }
}
