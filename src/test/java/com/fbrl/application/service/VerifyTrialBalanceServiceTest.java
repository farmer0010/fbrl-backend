package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase.TrialBalanceVerificationResult;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyTrialBalanceService 단위 테스트")
class VerifyTrialBalanceServiceTest {

  @Mock private LoadLedgerEntriesPort loadLedgerEntriesPort;
  @InjectMocks private VerifyTrialBalanceService verifyTrialBalanceService;

  @Test
  @DisplayName("총 차변과 총 대변이 같으면 대차평형이 유지된 것으로 판정한다.")
  void verify_balanced() {
    given(loadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.DEBIT))
        .willReturn(Money.wons(100000));
    given(loadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.CREDIT))
        .willReturn(Money.wons(100000));

    TrialBalanceVerificationResult result = verifyTrialBalanceService.verify();

    assertThat(result.balanced()).isTrue();
  }

  @Test
  @DisplayName("총 차변과 총 대변이 다르면 대차불일치로 판정한다.")
  void verify_unbalanced() {
    given(loadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.DEBIT))
        .willReturn(Money.wons(100000));
    given(loadLedgerEntriesPort.sumAmountByDirection(LedgerDirection.CREDIT))
        .willReturn(Money.wons(90000));

    TrialBalanceVerificationResult result = verifyTrialBalanceService.verify();

    assertThat(result.balanced()).isFalse();
    assertThat(result.totalDebit()).isEqualTo(Money.wons(100000));
    assertThat(result.totalCredit()).isEqualTo(Money.wons(90000));
  }
}
