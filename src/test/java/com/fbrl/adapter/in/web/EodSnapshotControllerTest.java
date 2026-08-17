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
@DisplayName("EodSnapshotController 날짜별 조회 통합 테스트")
class EodSnapshotControllerTest {

  private static final String USERNAME = "eod-date-test-admin";
  private static final String PASSWORD = "correct-password-123";

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
                "111-111", Money.wons(10_000_000), Money.wons(1000), LocalDate.of(2026, 8, 16)),
            EodSnapshot.of(
                "222-222", Money.wons(5_000_000), Money.wons(500), LocalDate.of(2026, 8, 16)),
            EodSnapshot.of(
                "333-333", Money.wons(7_000_000), Money.wons(700), LocalDate.of(2026, 8, 17))));
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
  @DisplayName("date로 조회하면 그 날짜에 정산된 모든 계좌의 스냅샷을 반환한다.")
  void getByDate_returnsAllAccountsForThatDate() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/eod-snapshots")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("date", "2026-08-16"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  @DisplayName("토큰 없이 조회를 시도하면 401을 반환한다.")
  void getByDate_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/eod-snapshots").param("date", "2026-08-16"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void getByDate_pageBeyondTotal_returnsEmptyContent() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/eod-snapshots")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("date", "2026-08-16")
                .param("page", "5")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(2));
  }
}
