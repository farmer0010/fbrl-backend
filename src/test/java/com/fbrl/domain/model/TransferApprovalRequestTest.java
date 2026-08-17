package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fbrl.domain.exception.InvalidApprovalTransitionException;
import com.fbrl.domain.exception.InvalidTransferAmountException;
import com.fbrl.domain.exception.RejectionReasonRequiredException;
import com.fbrl.domain.exception.SelfApprovalNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TransferApprovalRequest 도메인 모델 단위 테스트")
class TransferApprovalRequestTest {

  private static final String MAKER_ID = "maker-1";
  private static final String CHECKER_ID = "checker-1";

  @Test
  @DisplayName("요청 생성 시 PENDING 상태와 고유 requestId를 가진다.")
  void request_createsWithPendingStatus() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    assertThat(request.getRequestId()).isNotBlank();
    assertThat(request.getMakerId()).isEqualTo(MAKER_ID);
    assertThat(request.getCheckerId()).isNull();
    assertThat(request.getExecutionStatus()).isEqualTo(ExecutionStatus.NOT_APPLICABLE);
  }

  @Test
  @DisplayName("승인만 하고 아직 집행 결과를 기록하지 않으면 executionStatus는 NOT_APPLICABLE로 남는다.")
  void approve_withoutMarkingExecution_keepsExecutionStatusNotApplicable() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    request.approve(CHECKER_ID);

    assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(request.getExecutionStatus()).isEqualTo(ExecutionStatus.NOT_APPLICABLE);
  }

  @Test
  @DisplayName("거절된 요청은 executionStatus가 NOT_APPLICABLE로 남는다.")
  void reject_keepsExecutionStatusNotApplicable() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    request.reject(CHECKER_ID, "한도 초과 우려");

    assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
    assertThat(request.getExecutionStatus()).isEqualTo(ExecutionStatus.NOT_APPLICABLE);
  }

  @Test
  @DisplayName("markExecuted()를 호출하면 executionStatus가 EXECUTED가 되고 실패 사유는 비워진다.")
  void markExecuted_setsExecutedStatus() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));
    request.approve(CHECKER_ID);

    request.markExecuted();

    assertThat(request.getExecutionStatus()).isEqualTo(ExecutionStatus.EXECUTED);
    assertThat(request.getExecutionFailureReason()).isNull();
  }

  @Test
  @DisplayName("markExecutionFailed()를 호출하면 executionStatus가 FAILED가 되고 실패 사유가 기록된다.")
  void markExecutionFailed_setsFailedStatusWithReason() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));
    request.approve(CHECKER_ID);

    request.markExecutionFailed("이상거래로 의심되어 이체가 차단되었습니다.");

    assertThat(request.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(request.getExecutionFailureReason()).isEqualTo("이상거래로 의심되어 이체가 차단되었습니다.");
  }

  @Test
  @DisplayName("금액이 0 이하이면 InvalidTransferAmountException을 던진다.")
  void request_nonPositiveAmount_throwsException() {
    assertThatThrownBy(
            () -> TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.ZERO))
        .isInstanceOf(InvalidTransferAmountException.class);
  }

  @Test
  @DisplayName("승인하면 APPROVED 상태가 되고 checkerId/decidedAt이 기록된다.")
  void approve_success_transitionsToApproved() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    request.approve(CHECKER_ID);

    assertThat(request.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
    assertThat(request.getCheckerId()).isEqualTo(CHECKER_ID);
    assertThat(request.getDecidedAt()).isNotNull();
  }

  @Test
  @DisplayName("거절하면 REJECTED 상태가 되고 rejectionReason이 기록된다.")
  void reject_success_transitionsToRejected() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    request.reject(CHECKER_ID, "한도 초과 우려");

    assertThat(request.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
    assertThat(request.getRejectionReason()).isEqualTo("한도 초과 우려");
  }

  @Test
  @DisplayName("거절 사유가 없으면 RejectionReasonRequiredException을 던진다.")
  void reject_blankReason_throwsException() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    assertThatThrownBy(() -> request.reject(CHECKER_ID, "  "))
        .isInstanceOf(RejectionReasonRequiredException.class);
  }

  @Test
  @DisplayName("기안자 본인이 승인을 시도하면 SelfApprovalNotAllowedException을 던진다.")
  void approve_bySameMaker_throwsException() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    assertThatThrownBy(() -> request.approve(MAKER_ID))
        .isInstanceOf(SelfApprovalNotAllowedException.class);
  }

  @Test
  @DisplayName("기안자 본인이 거절을 시도하면 SelfApprovalNotAllowedException을 던진다.")
  void reject_bySameMaker_throwsException() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));

    assertThatThrownBy(() -> request.reject(MAKER_ID, "사유"))
        .isInstanceOf(SelfApprovalNotAllowedException.class);
  }

  @Test
  @DisplayName("이미 승인된 요청을 다시 거절하려 하면 InvalidApprovalTransitionException을 던진다.")
  void reject_alreadyApproved_throwsException() {
    TransferApprovalRequest request =
        TransferApprovalRequest.request(MAKER_ID, "111-111", "222-222", Money.wons(1_000_000));
    request.approve(CHECKER_ID);

    assertThatThrownBy(() -> request.reject(CHECKER_ID, "사유"))
        .isInstanceOf(InvalidApprovalTransitionException.class);
  }
}
