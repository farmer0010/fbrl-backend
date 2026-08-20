package com.fbrl.application.service;

import com.fbrl.application.port.in.DemoRejectTransferUseCase;
import com.fbrl.application.port.in.RejectTransferUseCase.ApprovalDecisionResult;
import com.fbrl.application.port.in.RejectTransferUseCase.RejectTransferCommand;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemoRejectTransferService implements DemoRejectTransferUseCase {

  private final LoadApprovalRequestPort demoLoadApprovalRequestPort;
  private final SaveApprovalRequestPort demoSaveApprovalRequestPort;

  public DemoRejectTransferService(
      @Qualifier("demo") LoadApprovalRequestPort demoLoadApprovalRequestPort,
      @Qualifier("demo") SaveApprovalRequestPort demoSaveApprovalRequestPort) {
    this.demoLoadApprovalRequestPort = demoLoadApprovalRequestPort;
    this.demoSaveApprovalRequestPort = demoSaveApprovalRequestPort;
  }

  @Override
  public ApprovalDecisionResult reject(RejectTransferCommand command) {
    TransferApprovalRequest request =
        demoLoadApprovalRequestPort
            .loadByRequestId(command.requestId())
            .orElseThrow(
                () ->
                    new ApprovalRequestNotFoundException(
                        "승인 요청을 찾을 수 없습니다. requestId: " + command.requestId()));

    request.reject(command.checkerId(), command.rejectionReason());
    TransferApprovalRequest saved = demoSaveApprovalRequestPort.save(request);

    return new ApprovalDecisionResult(saved.getRequestId(), saved.getStatus());
  }
}
