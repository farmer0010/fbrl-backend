package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.LoginUseCase.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "아이디는 필수입니다.") String username,
    @NotBlank(message = "비밀번호는 필수입니다.") String password) {
  public LoginCommand toCommand() {
    return new LoginCommand(username, password);
  }
}
