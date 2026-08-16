package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.LoginRequest;
import com.fbrl.adapter.in.web.dto.LoginResponse;
import com.fbrl.application.port.in.LoginUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final LoginUseCase loginUseCase;

  public AuthController(LoginUseCase loginUseCase) {
    this.loginUseCase = loginUseCase;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginUseCase.LoginResult result = loginUseCase.login(request.toCommand());
    return ResponseEntity.ok(new LoginResponse(result.token()));
  }
}
