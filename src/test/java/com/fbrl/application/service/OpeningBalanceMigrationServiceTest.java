package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.in.VerifyTrialBalanceUseCase.TrialBalanceVerificationResult;
import com.fbrl.application.service.OpeningBalanceMigrationService.LegacyAccountBalance;
import com.fbrl.domain.exception.ReservedAccountException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.SystemAccounts;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("OpeningBalanceMigrationService 통합 테스트")
class OpeningBalanceMigrationServiceTest {

  private static final String ACCOUNT_A = "111-111";
  private static final String ACCOUNT_B = "222-222";

  @Autowired private OpeningBalanceMigrationService openingBalanceMigrationService;
  @Autowired private AccountBalanceCalculator accountBalanceCalculator;
  @Autowired private VerifyTrialBalanceUseCase verifyTrialBalanceUseCase;
  @Autowired private TransferMoneyService transferMoneyService;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;

  @BeforeEach
  void setUp() {
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();

    accountPersistenceAdapter.save(Account.create(ACCOUNT_A));
    accountPersistenceAdapter.save(Account.create(ACCOUNT_B));
  }

  @Test
  @DisplayName("레거시 잔액을 시딩하면 각 계좌의 계산된 잔액이 레거시 값과 일치하고 대차평형이 유지된다.")
  void seedOpeningBalances_preservesBalancesAndKeepsTrialBalanced() {
    openingBalanceMigrationService.seedOpeningBalances(
        List.of(
            new LegacyAccountBalance(ACCOUNT_A, Money.wons(50_000)),
            new LegacyAccountBalance(ACCOUNT_B, Money.wons(30_000))));

    Money balanceA = accountBalanceCalculator.calculate(Account.create(ACCOUNT_A));
    Money balanceB = accountBalanceCalculator.calculate(Account.create(ACCOUNT_B));

    assertThat(balanceA).isEqualTo(Money.wons(50_000));
    assertThat(balanceB).isEqualTo(Money.wons(30_000));

    TrialBalanceVerificationResult result = verifyTrialBalanceUseCase.verify();
    assertThat(result.balanced()).isTrue();
    assertThat(result.totalDebit()).isEqualTo(result.totalCredit());
  }

  @Test
  @DisplayName("잔액이 0인 계좌는 시딩 대상에서 제외된다.")
  void seedOpeningBalances_skipsZeroBalance() {
    openingBalanceMigrationService.seedOpeningBalances(
        List.of(new LegacyAccountBalance(ACCOUNT_A, Money.ZERO)));

    Money balanceA = accountBalanceCalculator.calculate(Account.create(ACCOUNT_A));
    assertThat(balanceA).isEqualTo(Money.ZERO);
  }

  @Test
  @DisplayName("시스템 오프닝 밸런스 소스 계좌는 일반 이체의 송금/수취 대상으로 지정하면 명시적으로 차단된다.")
  void systemOpeningBalanceSourceAccount_cannotBeUsedAsTransferParty() {
    openingBalanceMigrationService.seedOpeningBalances(
        List.of(new LegacyAccountBalance(ACCOUNT_A, Money.wons(50_000))));

    TransferMoneyCommand command =
        new TransferMoneyCommand(
            SystemAccounts.OPENING_BALANCE_SOURCE, ACCOUNT_A, Money.wons(1_000));

    assertThatThrownBy(() -> transferMoneyService.transfer(command))
        .isInstanceOf(ReservedAccountException.class);
  }
}
