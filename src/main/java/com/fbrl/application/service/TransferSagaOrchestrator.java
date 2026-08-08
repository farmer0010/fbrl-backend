package com.fbrl.application.service;

import com.fbrl.application.port.in.StartTransferSagaUseCase;
import com.fbrl.application.port.out.DepositParticipantPort;
import com.fbrl.application.port.out.DepositParticipantPort.DepositResult;
import com.fbrl.application.port.out.WithdrawalParticipantPort;
import com.fbrl.application.port.out.WithdrawalParticipantPort.WithdrawalResult;
import com.fbrl.domain.model.TransferSaga;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferSagaOrchestrator implements StartTransferSagaUseCase {
  private final SagaStateWriter sagaStateWriter;
  private final WithdrawalParticipantPort withdrawalParticipantPort;
  private final DepositParticipantPort depositParticipantPort;

  @Override
  public TransferSagaResult startTransfer(StartTransferSagaCommand command) {
    TransferSaga saga =
        TransferSaga.start(
            command.fromAccountNumber(), command.toAccountNumber(), command.amount());
    sagaStateWriter.save(saga);

    WithdrawalResult withdrawalResult =
        withdrawalParticipantPort.withdraw(
            saga.getSagaId(), saga.getFromAccountNumber(), saga.getAmount());

    if (!withdrawalResult.success()) {
      saga.fail();
      sagaStateWriter.save(saga);
      return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
    }

    saga.completeWithdrawal();
    sagaStateWriter.save(saga);

    DepositResult depositResult =
        depositParticipantPort.deposit(
            saga.getSagaId(), saga.getToAccountNumber(), saga.getAmount());

    if (depositResult.success()) {
      saga.complete();
      sagaStateWriter.save(saga);
      return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
    }

    saga.startCompensation();
    sagaStateWriter.save(saga);

    DepositResult compensationResult =
        depositParticipantPort.deposit(
            saga.getSagaId(), saga.getFromAccountNumber(), saga.getAmount());

    if (compensationResult.success()) {
      saga.completeCompensation();
    } else {
      saga.fail();
    }
    sagaStateWriter.save(saga);

    return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
  }
}
