package com.fbrl.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.in.web.dto.TransferMoneyRequest;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.exception.InsufficientBalanceException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@DisplayName("TransferMoneyController 단위 테스트")
class TransferControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper = new ObjectMapper();

  private TransferMoneyUseCase transferMoneyUseCase;

  @BeforeEach
  void setUp() {
    transferMoneyUseCase = Mockito.mock(TransferMoneyUseCase.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TransferMoneyController(transferMoneyUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("정상적인 송금 요청 시 HTTP 200 OK를 반환한다.")
  void transferSuccess() throws Exception {
    TransferMoneyRequest request =
        new TransferMoneyRequest("111-111", "222-222", BigDecimal.valueOf(10000));

    willDoNothing().given(transferMoneyUseCase).transfer(any());

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("출금 계좌번호가 빈값이면 @Valid 검증에 실패하여 HTTP 400 Bad Request를 반환한다.")
  void transferValidationFailureWhenSenderBlank() throws Exception {
    TransferMoneyRequest request =
        new TransferMoneyRequest("", "222-222", BigDecimal.valueOf(10000));

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.message").value("출금 계좌번호는 필수입니다."));
  }

  @Test
  @DisplayName("존재하지 않는 계좌로 송금 시 AccountNotFoundException이 터지면 404 Not Found를 반환한다.")
  void transferAccountNotFound() throws Exception {
    TransferMoneyRequest request =
        new TransferMoneyRequest("111-111", "999-999", BigDecimal.valueOf(10000));

    willThrow(new AccountNotFoundException("입금 계좌를 찾을 수 없습니다."))
        .given(transferMoneyUseCase)
        .transfer(any());

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("입금 계좌를 찾을 수 없습니다."));
  }

  @Test
  @DisplayName("잔액이 부족하여 InsufficientBalanceException이 터지면 400 Bad Request를 반환한다.")
  void transferInsufficientBalance() throws Exception {
    TransferMoneyRequest request =
        new TransferMoneyRequest("111-111", "222-222", BigDecimal.valueOf(1000000));

    willThrow(new InsufficientBalanceException("잔액이 부족합니다."))
        .given(transferMoneyUseCase)
        .transfer(any());

    mockMvc
        .perform(
            post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"))
        .andExpect(jsonPath("$.message").value("잔액이 부족합니다."));
  }
}
