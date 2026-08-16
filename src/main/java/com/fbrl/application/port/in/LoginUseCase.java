package com.fbrl.application.port.in;

public interface LoginUseCase {
  LoginResult login(LoginCommand command);

  record LoginCommand(String username, String password) {}

  record LoginResult(String token) {}
}
