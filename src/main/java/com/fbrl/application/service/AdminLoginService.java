package com.fbrl.application.service;

import com.fbrl.application.port.in.LoginUseCase;
import com.fbrl.application.port.out.LoadAdminUserPort;
import com.fbrl.application.port.out.TokenPort;
import com.fbrl.domain.exception.InvalidCredentialsException;
import com.fbrl.domain.model.AdminUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AdminLoginService implements LoginUseCase {

  private final AuthenticationManager authenticationManager;
  private final LoadAdminUserPort loadAdminUserPort;
  private final TokenPort tokenPort;

  public AdminLoginService(
      AuthenticationManager authenticationManager,
      LoadAdminUserPort loadAdminUserPort,
      TokenPort tokenPort) {
    this.authenticationManager = authenticationManager;
    this.loadAdminUserPort = loadAdminUserPort;
    this.tokenPort = tokenPort;
  }

  @Override
  public LoginResult login(LoginCommand command) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(command.username(), command.password()));
    } catch (AuthenticationException e) {
      throw new InvalidCredentialsException();
    }

    AdminUser adminUser =
        loadAdminUserPort
            .loadByUsername(command.username())
            .orElseThrow(InvalidCredentialsException::new);

    return new LoginResult(tokenPort.issueToken(adminUser));
  }
}
