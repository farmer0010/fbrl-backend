package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetApprovalRequestService 단위 테스트")
class GetApprovalRequestServiceTest {

  @Mock private LoadApprovalRequestPort loadApprovalRequestPort;

  private GetApprovalRequestService sut;

  @Test
  @DisplayName("존재하는 requestId로 조회하면 해당 승인 요청을 반환한다.")
  void getByRequestId_found_returnsRequest() {
    sut = new GetApprovalRequestService(loadApprovalRequestPort);

    TransferApprovalRequest request =
        TransferApprovalRequest.request("maker-1", "111-111", "222-222", Money.wons(20_000_000));
    given(loadApprovalRequestPort.loadByRequestId(request.getRequestId()))
        .willReturn(Optional.of(request));

    TransferApprovalRequest result = sut.getByRequestId(request.getRequestId());

    assertThat(result.getRequestId()).isEqualTo(request.getRequestId());
  }

  @Test
  @DisplayName("존재하지 않는 requestId면 ApprovalRequestNotFoundException을 던진다.")
  void getByRequestId_notFound_throwsException() {
    sut = new GetApprovalRequestService(loadApprovalRequestPort);

    given(loadApprovalRequestPort.loadByRequestId("missing")).willReturn(Optional.empty());

    assertThatThrownBy(() -> sut.getByRequestId("missing"))
        .isInstanceOf(ApprovalRequestNotFoundException.class);
  }
}
