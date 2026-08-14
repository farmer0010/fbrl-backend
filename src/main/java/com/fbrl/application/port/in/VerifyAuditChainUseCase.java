package com.fbrl.application.port.in;

public interface VerifyAuditChainUseCase {
  AuditChainVerificationResult verify();

  record AuditChainVerificationResult(
      boolean valid, long totalEntries, Long brokenAtId, String reason) {

    public static AuditChainVerificationResult valid(long totalEntries) {
      return new AuditChainVerificationResult(true, totalEntries, null, null);
    }

    public static AuditChainVerificationResult broken(
        long totalEntries, Long brokenAtId, String reason) {
      return new AuditChainVerificationResult(false, totalEntries, brokenAtId, reason);
    }
  }
}
