package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.application.port.in.StartTransferSagaUseCase.StartTransferSagaCommand;
import com.fbrl.application.port.in.StartTransferSagaUseCase.TransferSagaResult;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.application.service.TransferSagaOrchestrator;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.SagaStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("TransferSagaOrchestrator 통합 테스트 — 실제 JPA로 saga 반복 저장 시 id 유지 확인")
class TransferSagaOrchestratorIntegrationTest {

  @Autowired private TransferSagaOrchestrator transferSagaOrchestrator;
  @Autowired private TransferSagaJpaRepository transferSagaJpaRepository;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;
  @Autowired private LedgerEntryPersistenceAdapter ledgerEntryPersistenceAdapter;
  @Autowired private SaveLedgerEntryPort saveLedgerEntryPort;

  private static final String SENDER_ACCOUNT_NUMBER = "SAGA-FIX-111";
  private static final String RECEIVER_ACCOUNT_NUMBER = "SAGA-FIX-222";

  @BeforeEach
  void setUp() {
    transferSagaJpaRepository.deleteAllInBatch();
    ledgerEntryPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();

    accountPersistenceAdapter.save(Account.create(SENDER_ACCOUNT_NUMBER));
    accountPersistenceAdapter.save(Account.create(RECEIVER_ACCOUNT_NUMBER));

    saveLedgerEntryPort.saveAll(
        List.of(
            LedgerEntry.of(
                SENDER_ACCOUNT_NUMBER,
                LedgerDirection.CREDIT,
                Money.wons(1_000_000),
                "SAGA_FIX_SEED",
                Instant.now())));
  }

  @Test
  @DisplayName(
      "출금→입금 성공 흐름에서 saga가 여러 번(3회) 저장되는 동안 saga_id 유니크 제약 위반 없이 전부 성공하고, "
          + "매 저장마다 갱신된 id로 실제 DB의 동일 행이 업데이트된다(id가 null로 되돌아가지 않는다).")
  void startTransfer_success_savesRepeatedlyWithoutUniqueConstraintViolation() {
    TransferSagaResult result =
        transferSagaOrchestrator.startTransfer(
            new StartTransferSagaCommand(
                SENDER_ACCOUNT_NUMBER, RECEIVER_ACCOUNT_NUMBER, Money.wons(1000)));

    assertThat(result.finalStatus()).isEqualTo(SagaStatus.COMPLETED);

    List<TransferSagaJpaEntity> rows =
        transferSagaJpaRepository.findAll().stream()
            .filter(e -> e.getSagaId().equals(result.sagaId()))
            .toList();

    assertThat(rows).hasSize(1);

    TransferSagaJpaEntity saved = rows.get(0);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStatus()).isEqualTo(SagaStatus.COMPLETED);
    assertThat(saved.getVersion()).isEqualTo(2L);
  }
}
