package com.fbrl.application.service;

import com.fbrl.application.port.in.StartTransferSagaUseCase;
import com.fbrl.application.port.out.DepositParticipantPort;
import com.fbrl.application.port.out.DepositParticipantPort.DepositResult;
import com.fbrl.application.port.out.WithdrawalParticipantPort;
import com.fbrl.application.port.out.WithdrawalParticipantPort.WithdrawalResult;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferSaga;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferSagaOrchestrator implements StartTransferSagaUseCase {
  private static final String SPAN_WITHDRAWAL = "saga.withdrawal";
  private static final String SPAN_DEPOSIT = "saga.deposit";
  private static final String SPAN_COMPENSATION = "saga.compensation";

  private final SagaStateWriter sagaStateWriter;
  private final WithdrawalParticipantPort withdrawalParticipantPort;
  private final DepositParticipantPort depositParticipantPort;
  private final Tracer tracer;

  @Override
  public TransferSagaResult startTransfer(StartTransferSagaCommand command) {
    TransferSaga saga =
        TransferSaga.start(
            command.fromAccountNumber(), command.toAccountNumber(), command.amount());
    saga = sagaStateWriter.save(saga);

    final String sagaId = saga.getSagaId();
    final String fromAccountNumber = saga.getFromAccountNumber();
    final String toAccountNumber = saga.getToAccountNumber();
    final Money amount = saga.getAmount();

    WithdrawalResult withdrawalResult =
        traced(
            SPAN_WITHDRAWAL,
            () -> withdrawalParticipantPort.withdraw(sagaId, fromAccountNumber, amount));

    if (!withdrawalResult.success()) {
      saga.fail();
      saga = sagaStateWriter.save(saga);
      return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
    }

    saga.completeWithdrawal();
    saga = sagaStateWriter.save(saga);

    DepositResult depositResult =
        traced(SPAN_DEPOSIT, () -> depositParticipantPort.deposit(sagaId, toAccountNumber, amount));

    if (depositResult.success()) {
      saga.complete();
      saga = sagaStateWriter.save(saga);
      return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
    }

    saga.startCompensation();
    saga = sagaStateWriter.save(saga);

    DepositResult compensationResult =
        traced(
            SPAN_COMPENSATION,
            () -> depositParticipantPort.deposit(sagaId, fromAccountNumber, amount));

    if (compensationResult.success()) {
      saga.completeCompensation();
    } else {
      saga.fail();
    }
    saga = sagaStateWriter.save(saga);

    return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
  }

  private <T> T traced(String spanName, Supplier<T> action) {
    Span span = tracer.nextSpan().name(spanName).start();
    try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
      return action.get();
    } catch (RuntimeException e) {
      span.error(e);
      throw e;
    } finally {
      span.end();
    }
  }
}
