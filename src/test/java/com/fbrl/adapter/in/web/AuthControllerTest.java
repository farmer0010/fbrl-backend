package com.fbrl.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.application.port.in.LoginUseCase;
import com.fbrl.application.port.in.LoginUseCase.LoginCommand;
import com.fbrl.application.port.in.LoginUseCase.LoginResult;
import com.fbrl.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

  private MockMvc mockMvc;
  private LoginUseCase loginUseCase;

  @BeforeEach
  void setUp() {
    loginUseCase = Mockito.mock(LoginUseCase.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuthController(loginUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("아이디/비밀번호가 맞으면 토큰을 반환한다.")
  void login_success_returnsToken() throws Exception {
    given(loginUseCase.login(new LoginCommand("admin", "password123")))
        .willReturn(new LoginResult("issued-jwt-token"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("issued-jwt-token"));
  }

  @Test
  @DisplayName("아이디/비밀번호가 틀리면 401을 반환한다.")
  void login_invalidCredentials_returns401() throws Exception {
    given(loginUseCase.login(new LoginCommand("admin", "wrong-password")))
        .willThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  @DisplayName("아이디/비밀번호가 비어있으면 400을 반환한다.")
  void login_blankFields_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
