package com.fbrl.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.out.persistence.AdminUserPersistenceAdapter;
import com.fbrl.application.port.out.SaveAdminUserPort;
import com.fbrl.domain.model.AdminUser;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@SpringBatchTest
@AutoConfigureMockMvc
@DisplayName("BatchJobExecutionController 통합 테스트")
class BatchJobExecutionControllerTest {

  private static final String USERNAME = "batch-history-test-admin";
  private static final String PASSWORD = "correct-password-123";
  private static final String JOB_NAME = "reportTestJob";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SaveAdminUserPort saveAdminUserPort;
  @Autowired private AdminUserPersistenceAdapter adminUserPersistenceAdapter;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JobRepository jobRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String token;

  @BeforeEach
  void setUp() throws Exception {
    jdbcTemplate.execute(
        "TRUNCATE TABLE batch_job_execution_context, batch_step_execution_context, "
            + "batch_step_execution, batch_job_execution_params, batch_job_execution, "
            + "batch_job_instance RESTART IDENTITY CASCADE");
    adminUserPersistenceAdapter.deleteAllInBatch();
    saveAdminUserPort.save(AdminUser.create(USERNAME, passwordEncoder.encode(PASSWORD)));
    token = login();

    createExecution(JOB_NAME, BatchStatus.COMPLETED);
  }

  private void createExecution(String jobName, BatchStatus status) {
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("runId", UUID.randomUUID().toString())
            .toJobParameters();
    JobInstance jobInstance = jobRepository.createJobInstance(jobName, jobParameters);
    JobExecution jobExecution =
        jobRepository.createJobExecution(jobInstance, jobParameters, new ExecutionContext());
    jobExecution.setStatus(status);
    jobExecution.setStartTime(LocalDateTime.now());
    jobExecution.setEndTime(LocalDateTime.now());
    jobExecution.setExitStatus(ExitStatus.COMPLETED);
    jobRepository.update(jobExecution);
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
  @DisplayName("jobName으로 실행 이력을 조회하면 200 OK와 함께 목록을 반환한다.")
  void getExecutions_returnsHistory() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/batch-jobs/" + JOB_NAME + "/executions")
                .header(HttpHeaders.AUTHORIZATION, bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].jobName").value(JOB_NAME))
        .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
  }

  @Test
  @DisplayName("토큰 없이 조회를 시도하면 401을 반환한다.")
  void getExecutions_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/batch-jobs/" + JOB_NAME + "/executions"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void getExecutions_pageBeyondTotal_returnsEmptyContent() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/batch-jobs/" + JOB_NAME + "/executions")
                .header(HttpHeaders.AUTHORIZATION, bearer())
                .param("page", "5")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(1));
  }
}
