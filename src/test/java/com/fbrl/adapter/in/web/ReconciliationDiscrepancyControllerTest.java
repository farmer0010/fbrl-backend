package com.fbrl.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.adapter.out.persistence.ReconciliationDiscrepancyPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
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
@DisplayName("ReconciliationDiscrepancyController 통합 테스트")
class ReconciliationDiscrepancyControllerTest {

  private static final String USERNAME = "reconciliation-test-admin";
  private static final String PASSWORD = "correct-password-123";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired
  private ReconciliationDiscrepancyPersistenceAdapter reconciliationDiscrepancyPersistenceAdapter;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    reconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();
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
  @DisplayName("status/기간 필터로 조회하면 해당 조건에 맞는 페이지를 반환한다.")
  void search_withFilters_returnsMatchingPage() throws Exception {
    LocalDate day1 = LocalDate.of(2026, 8, 1);
    LocalDate day2 = LocalDate.of(2026, 8, 2);
    reconciliationDiscrepancyPersistenceAdapter.saveAll(
        List.of(
            ReconciliationDiscrepancy.mismatch("111-111", day1, Money.wons(9000), Money.wons(8500)),
            ReconciliationDiscrepancy.noSnapshot("222-222", day2)));

    mockMvc
        .perform(
            get("/api/v1/reconciliation-discrepancies")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("status", "MISMATCH")
                .param("from", day1.toString())
                .param("to", day2.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].accountNumber").value("111-111"))
        .andExpect(jsonPath("$.content[0].status").value("MISMATCH"));
  }

  @Test
  @DisplayName("토큰 없이 조회를 시도하면 401을 반환한다.")
  void search_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/reconciliation-discrepancies")
                .param("from", "2026-08-01")
                .param("to", "2026-08-02"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void search_pageBeyondTotal_returnsEmptyContent() throws Exception {
    LocalDate day = LocalDate.of(2026, 8, 1);
    reconciliationDiscrepancyPersistenceAdapter.saveAll(
        List.of(ReconciliationDiscrepancy.noSnapshot("111-111", day)));

    mockMvc
        .perform(
            get("/api/v1/reconciliation-discrepancies")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("from", day.toString())
                .param("to", day.toString())
                .param("page", "5")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(1));
  }
}
