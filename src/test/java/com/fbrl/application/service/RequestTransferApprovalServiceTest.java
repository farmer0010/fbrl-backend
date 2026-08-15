package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.RequestTransferApprovalCommand;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalNotRequiredException;
import com.fbrl.domain.model.ApprovalPolicy;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestTransferApprovalService 단위 테스트")
class RequestTransferApprovalServiceTest {

  @Mock private SaveApprovalRequestPort saveApprovalRequestPort;

  private final ApprovalPolicy approvalPolicy = new ApprovalPolicy(Money.wons(10_000_000));

  @Test
  @DisplayName("threshold 이상 금액이면 PENDING 상태로 승인 요청을 저장한다.")
  void requestApproval_aboveThreshold_savesPendingRequest() {
    given(saveApprovalRequestPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    RequestTransferApprovalService sut =
        new RequestTransferApprovalService(approvalPolicy, saveApprovalRequestPort);

    ApprovalRequestResult result =
        sut.requestApproval(
            new RequestTransferApprovalCommand(
                "maker-1", "111-111", "222-222", Money.wons(20_000_000)));

    assertThat(result.status()).isEqualTo(ApprovalStatus.PENDING);
    verify(saveApprovalRequestPort).save(any(TransferApprovalRequest.class));
  }

  @Test
  @DisplayName("threshold 미만 금액이면 ApprovalNotRequiredException을 던지고 저장하지 않는다.")
  void requestApproval_belowThreshold_throwsException() {
    RequestTransferApprovalService sut =
        new RequestTransferApprovalService(approvalPolicy, saveApprovalRequestPort);

    assertThatThrownBy(
            () ->
                sut.requestApproval(
                    new RequestTransferApprovalCommand(
                        "maker-1", "111-111", "222-222", Money.wons(1_000_000))))
        .isInstanceOf(ApprovalNotRequiredException.class);

    verify(saveApprovalRequestPort, never()).save(any());
  }
}
