package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.AuditChainVerificationResponse;
import com.fbrl.application.port.in.VerifyAuditChainUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

  private final VerifyAuditChainUseCase verifyAuditChainUseCase;

  public AuditController(VerifyAuditChainUseCase verifyAuditChainUseCase) {
    this.verifyAuditChainUseCase = verifyAuditChainUseCase;
  }

  @GetMapping("/verify")
  public ResponseEntity<AuditChainVerificationResponse> verify() {
    var result = verifyAuditChainUseCase.verify();
    return ResponseEntity.ok(AuditChainVerificationResponse.from(result));
  }
}
