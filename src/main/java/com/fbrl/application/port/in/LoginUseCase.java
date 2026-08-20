package com.fbrl.application.port.in;

import com.fbrl.domain.model.AdminRole;

public interface LoginUseCase {
  LoginResult login(LoginCommand command);

  record LoginCommand(String username, String password) {}

  record LoginResult(String token, AdminRole role) {}
}
