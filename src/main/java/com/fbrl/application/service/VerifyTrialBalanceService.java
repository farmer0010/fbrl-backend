package com.fbrl.application.service;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyTrialBalanceService implements VerifyTrialBalanceUseCase {

  private final LoadLedgerEntriesPort loadLedgerEntriesPort;

  @Override
  @Transactional(readOnly = true)
  public TrialBalanceVerificationResult verify() {
    Money totalDebit = loadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.DEBIT);
    Money totalCredit = loadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.CREDIT);
    return TrialBalanceVerificationResult.of(totalDebit, totalCredit);
  }
}
