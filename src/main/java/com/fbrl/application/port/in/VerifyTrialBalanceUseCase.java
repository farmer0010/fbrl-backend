package com.fbrl.application.port.in;

import com.fbrl.domain.model.Money;

public interface VerifyTrialBalanceUseCase {
  TrialBalanceVerificationResult verify();

  record TrialBalanceVerificationResult(boolean balanced, Money totalDebit, Money totalCredit) {
    public static TrialBalanceVerificationResult of(Money totalDebit, Money totalCredit) {
      return new TrialBalanceVerificationResult(
          totalDebit.equals(totalCredit), totalDebit, totalCredit);
    }
  }
}
