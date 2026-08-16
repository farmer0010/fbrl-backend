package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.adapter.out.persistence.ApprovalPersistenceAdapter;
import com.fbrl.adapter.out.persistence.EodSnapshotPersistenceAdapter;
import com.fbrl.adapter.out.persistence.LedgerEntryPersistenceAdapter;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.ExecutionStatus;
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
@DisplayName("승인 후 이체는 웹 게이트(TransferMoneyController)를 거치지 않고 실제로 실행된다")
class ApproveTransferBypassesWebGateIntegrationTest {

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
                Money.wons(50_000_000),
                "TEST_SEED",
                Instant.now())));
  }

  @Test
  @DisplayName("threshold(1000만원) 이상 금액도 승인 완료 후에는 실제로 이체가 실행된다.")
  void approve_aboveThreshold_actuallyMovesMoney() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request("maker-1", SENDER, RECEIVER, Money.wons(20_000_000));
    approvalPersistenceAdapter.save(request);

    approveTransferService.approve(new ApproveTransferCommand(request.getRequestId(), "checker-1"));

    assertThat(accountBalanceCalculator.calculate(Account.create(SENDER)))
        .isEqualTo(Money.wons(30_000_000));
    assertThat(accountBalanceCalculator.calculate(Account.create(RECEIVER)))
        .isEqualTo(Money.wons(20_000_000));

    TransferApprovalRequest persisted =
        approvalPersistenceAdapter.loadByRequestId(request.getRequestId()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(persisted.getExecutionStatus()).isEqualTo(ExecutionStatus.EXECUTED);
    assertThat(persisted.getExecutionFailureReason()).isNull();
  }
}
