package com.fbrl.application.service;

import com.fbrl.application.port.in.StartTransferSagaUseCase;
import com.fbrl.application.port.out.DepositParticipantPort;
import com.fbrl.application.port.out.DepositParticipantPort.DepositResult;
import com.fbrl.application.port.out.WithdrawalParticipantPort;
import com.fbrl.application.port.out.WithdrawalParticipantPort.WithdrawalResult;
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
    sagaStateWriter.save(saga);

    WithdrawalResult withdrawalResult =
        traced(
            SPAN_WITHDRAWAL,
            () ->
                withdrawalParticipantPort.withdraw(
                    saga.getSagaId(), saga.getFromAccountNumber(), saga.getAmount()));

    if (!withdrawalResult.success()) {
      saga.fail();
      sagaStateWriter.save(saga);
      return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
    }

    saga.completeWithdrawal();
    sagaStateWriter.save(saga);

    DepositResult depositResult =
        traced(
            SPAN_DEPOSIT,
            () ->
                depositParticipantPort.deposit(
                    saga.getSagaId(), saga.getToAccountNumber(), saga.getAmount()));

    if (depositResult.success()) {
      saga.complete();
      sagaStateWriter.save(saga);
      return new TransferSagaResult(saga.getSagaId(), saga.getStatus());
    }

    saga.startCompensation();
    sagaStateWriter.save(saga);

    DepositResult compensationResult =
        traced(
            SPAN_COMPENSATION,
            () ->
                depositParticipantPort.deposit(
                    saga.getSagaId(), saga.getFromAccountNumber(), saga.getAmount()));

    if (compensationResult.success()) {
      saga.completeCompensation();
    } else {
      saga.fail();
    }
    sagaStateWriter.save(saga);

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
