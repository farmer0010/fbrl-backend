package com.fbrl.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.application.port.in.CreateAccountUseCase;
import com.fbrl.application.port.in.GetAccountUseCase;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("AccountController 단위 테스트")
class AccountControllerTest {

  private MockMvc mockMvc;
  private CreateAccountUseCase createAccountUseCase;
  private GetAccountUseCase getAccountUseCase;

  @BeforeEach
  void setUp() {
    createAccountUseCase = Mockito.mock(CreateAccountUseCase.class);
    getAccountUseCase = Mockito.mock(GetAccountUseCase.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new AccountController(createAccountUseCase, getAccountUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("계좌 생성 요청 시 201 Created와 함께 계좌 정보를 반환한다.")
  void createAccountSuccess() throws Exception {
    given(createAccountUseCase.createAccount()).willReturn(Account.open("110-0001-0001"));

    mockMvc
        .perform(post("/api/v1/accounts"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountNumber").value("110-0001-0001"))
        .andExpect(jsonPath("$.balance").value(0));
  }

  @Test
  @DisplayName("존재하는 계좌번호로 조회하면 200 OK와 함께 계좌 정보를 반환한다.")
  void getAccountSuccess() throws Exception {
    given(getAccountUseCase.getAccount("110-0001-0001")).willReturn(Account.open("110-0001-0001"));

    mockMvc
        .perform(get("/api/v1/accounts/110-0001-0001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountNumber").value("110-0001-0001"));
  }

  @Test
  @DisplayName("존재하지 않는 계좌를 조회하면 404 Not Found를 반환한다.")
  void getAccountNotFound() throws Exception {
    given(getAccountUseCase.getAccount("999-9999-9999"))
        .willThrow(new AccountNotFoundException("계좌를 찾을 수 없습니다."));

    mockMvc
        .perform(get("/api/v1/accounts/999-9999-9999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
  }
}
