package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.ApprovalPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.exception.SuspiciousTransferException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("승인 후 이체 트리거 경로도 이상거래 탐지를 우회하지 못한다")
class ApproveTransferTriggersFraudCheckIntegrationTest {

  private static final String SENDER = "111-111";
  private static final String RECEIVER = "222-222";

  @Autowired private ApproveTransferService approveTransferService;
  @Autowired private ApprovalPersistenceAdapter approvalPersistenceAdapter;
  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private EodSnapshotPersistenceAdapter eodSnapshotPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;
  @Autowired private AccountBalanceCalculator accountBalanceCalculator;

  @BeforeEach
  void setUp() {
    approvalPersistenceAdapter.deleteAllInBatch();
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    eodSnapshotPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();

    accountPersistenceAdapter.save(Account.create(SENDER));
    accountPersistenceAdapter.save(Account.create(RECEIVER));
    saveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER,
                LedgerDirection.CREDIT,
                Money.wons(100_000_000),
                "TEST_SEED",
                Instant.now())));
  }

  @Test
  @DisplayName(
      "fraud threshold(5천만원) 이상 금액은 승인 완료 후에도 SuspiciousTransferException으로 차단되고 잔액이 이동하지 않는다.")
  void approve_aboveFraudThreshold_isBlockedEvenAfterApproval() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request("maker-1", SENDER, RECEIVER, Money.wons(60_000_000));
    approvalPersistenceAdapter.save(request);

    assertThatThrownBy(
            () ->
                approveTransferService.approve(
                    new ApproveTransferCommand(request.getRequestId(), "checker-1")))
        .isInstanceOf(SuspiciousTransferException.class);

    assertThat(accountBalanceCalculator.calculate(Account.create(SENDER)))
        .isEqualTo(Money.wons(100_000_000));
    assertThat(accountBalanceCalculator.calculate(Account.create(RECEIVER)))
        .isEqualTo(Money.wons(0));
  }
}
