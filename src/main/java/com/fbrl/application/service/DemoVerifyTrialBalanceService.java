package com.fbrl.application.service;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.Money;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Qualifier("demo")
public class DemoVerifyTrialBalanceService implements VerifyTrialBalanceUseCase {

  private final LoadLedgerEntriesPort demoLoadLedgerEntriesPort;

  public DemoVerifyTrialBalanceService(
      @Qualifier("demo") LoadLedgerEntriesPort demoLoadLedgerEntriesPort) {
    this.demoLoadLedgerEntriesPort = demoLoadLedgerEntriesPort;
  }

  @Override
  @Transactional(value = "demoTransactionManager", readOnly = true)
  public TrialBalanceVerificationResult verify() {
    Money totalDebit = demoLoadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.DEBIT);
    Money totalCredit = demoLoadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.CREDIT);
    return TrialBalanceVerificationResult.of(totalDebit, totalCredit);
  }
}
