package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.fbrl.application.port.in.StartTransferSagaUseCase.StartTransferSagaCommand;
import com.fbrl.application.port.in.StartTransferSagaUseCase.TransferSagaResult;
import com.fbrl.application.port.out.DepositParticipantPort;
import com.fbrl.application.port.out.DepositParticipantPort.DepositResult;
import com.fbrl.application.port.out.WithdrawalParticipantPort;
import com.fbrl.application.port.out.WithdrawalParticipantPort.WithdrawalResult;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.SagaStatus;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferSagaOrchestrator 단위 테스트")
class TransferSagaOrchestratorTest {

  @Mock private SagaStateWriter sagaStateWriter;
  @Mock private WithdrawalParticipantPort withdrawalParticipantPort;
  @Mock private DepositParticipantPort depositParticipantPort;
  @Mock private Tracer tracer;

  private TransferSagaOrchestrator orchestrator;

  @BeforeEach
  void setUp() {
    orchestrator =
        new TransferSagaOrchestrator(
            sagaStateWriter, withdrawalParticipantPort, depositParticipantPort, tracer);

    given(tracer.nextSpan())
        .willAnswer(
            invocation -> {
              Span span = mock(Span.class);
              given(span.name(any())).willReturn(span);
              given(span.start()).willReturn(span);
              return span;
            });
    given(tracer.withSpan(any())).willReturn(mock(Tracer.SpanInScope.class));
    given(sagaStateWriter.save(any())).willAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  @DisplayName("정상 이체는 saga.withdrawal → saga.deposit 순으로 span을 열고 닫는다.")
  void startTransfer_success_createsWithdrawalAndDepositSpans() {
    given(withdrawalParticipantPort.withdraw(any(), any(), any()))
        .willReturn(new WithdrawalResult(true, null));
    given(depositParticipantPort.deposit(any(), any(), any()))
        .willReturn(new DepositResult(true, null));

    TransferSagaResult result =
        orchestrator.startTransfer(
            new StartTransferSagaCommand("111-111", "222-222", Money.of(BigDecimal.valueOf(1000))));

    assertThat(result.finalStatus()).isEqualTo(SagaStatus.COMPLETED);

    InOrder inOrder = inOrder(withdrawalParticipantPort, depositParticipantPort, tracer);
    inOrder.verify(tracer).nextSpan();
    inOrder.verify(withdrawalParticipantPort).withdraw(any(), any(), any());
    inOrder.verify(tracer).nextSpan();
    inOrder.verify(depositParticipantPort).deposit(any(), any(), any());
  }

  @Test
  @DisplayName("입금 실패 시 보상 트랜잭션도 saga.compensation span으로 감싸 실행한다.")
  void startTransfer_depositFails_wrapsCompensationInSpan() {
    given(withdrawalParticipantPort.withdraw(any(), any(), any()))
        .willReturn(new WithdrawalResult(true, null));
    given(depositParticipantPort.deposit(any(), any(), any()))
        .willReturn(new DepositResult(false, "입금 실패"))
        .willReturn(new DepositResult(true, null));

    TransferSagaResult result =
        orchestrator.startTransfer(
            new StartTransferSagaCommand("111-111", "222-222", Money.of(BigDecimal.valueOf(1000))));

    assertThat(result.finalStatus()).isEqualTo(SagaStatus.COMPENSATED);

    org.mockito.Mockito.verify(tracer, org.mockito.Mockito.times(3)).nextSpan();
    org.mockito.Mockito.verify(depositParticipantPort, org.mockito.Mockito.times(2))
        .deposit(any(), any(), any());
  }

  @Test
  @DisplayName("출금 자체가 실패하면 saga.withdrawal span만 열리고 deposit span은 열리지 않는다.")
  void startTransfer_withdrawalFails_onlyOpensWithdrawalSpan() {
    given(withdrawalParticipantPort.withdraw(any(), any(), any()))
        .willReturn(new WithdrawalResult(false, "잔액 부족"));

    TransferSagaResult result =
        orchestrator.startTransfer(
            new StartTransferSagaCommand("111-111", "222-222", Money.of(BigDecimal.valueOf(1000))));

    assertThat(result.finalStatus()).isEqualTo(SagaStatus.FAILED);
    org.mockito.Mockito.verify(tracer, org.mockito.Mockito.times(1)).nextSpan();
    org.mockito.Mockito.verify(depositParticipantPort, org.mockito.Mockito.never())
        .deposit(any(), any(), any());
  }
}
