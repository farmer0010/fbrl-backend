package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.in.RejectTransferUseCase.ApprovalDecisionResult;
import com.fbrl.application.port.in.RejectTransferUseCase.RejectTransferCommand;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.exception.RejectionReasonRequiredException;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RejectTransferService 단위 테스트")
class RejectTransferServiceTest {

  @Mock private LoadApprovalRequestPort loadApprovalRequestPort;
  @Mock private SaveApprovalRequestPort saveApprovalRequestPort;

  private RejectTransferService sut;

  @Test
  @DisplayName("정상 거절 시 상태를 REJECTED로 저장한다.")
  void reject_success_savesRejectedRequest() {
    sut = new RejectTransferService(loadApprovalRequestPort, saveApprovalRequestPort);

    TransferApprovalRequest request =
        TransferApprovalRequest.request("maker-1", "111-111", "222-222", Money.wons(20_000_000));
    given(loadApprovalRequestPort.loadByRequestId(request.getRequestId()))
        .willReturn(Optional.of(request));
    given(saveApprovalRequestPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    ApprovalDecisionResult result =
        sut.reject(new RejectTransferCommand(request.getRequestId(), "checker-1", "한도 초과 우려"));

    assertThat(result.status()).isEqualTo(ApprovalStatus.REJECTED);
  }

  @Test
  @DisplayName("존재하지 않는 requestId면 ApprovalRequestNotFoundException을 던진다.")
  void reject_requestNotFound_throwsException() {
    sut = new RejectTransferService(loadApprovalRequestPort, saveApprovalRequestPort);

    given(loadApprovalRequestPort.loadByRequestId("missing")).willReturn(Optional.empty());

    assertThatThrownBy(() -> sut.reject(new RejectTransferCommand("missing", "checker-1", "사유")))
        .isInstanceOf(ApprovalRequestNotFoundException.class);
  }

  @Test
  @DisplayName("거절 사유가 없으면 RejectionReasonRequiredException을 던진다.")
  void reject_blankReason_throwsException() {
    sut = new RejectTransferService(loadApprovalRequestPort, saveApprovalRequestPort);

    TransferApprovalRequest request =
        TransferApprovalRequest.request("maker-1", "111-111", "222-222", Money.wons(20_000_000));
    given(loadApprovalRequestPort.loadByRequestId(request.getRequestId()))
        .willReturn(Optional.of(request));

    assertThatThrownBy(
            () -> sut.reject(new RejectTransferCommand(request.getRequestId(), "checker-1", " ")))
        .isInstanceOf(RejectionReasonRequiredException.class);
  }
}
