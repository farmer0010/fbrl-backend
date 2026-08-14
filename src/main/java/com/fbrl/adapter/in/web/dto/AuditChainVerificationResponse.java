package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.VerifyAuditChainUseCase.AuditChainVerificationResult;

public record AuditChainVerificationResponse(
    boolean valid, long totalEntries, Long brokenAtId, String reason) {

  public static AuditChainVerificationResponse from(AuditChainVerificationResult result) {
    return new AuditChainVerificationResponse(
        result.valid(), result.totalEntries(), result.brokenAtId(), result.reason());
  }
}
