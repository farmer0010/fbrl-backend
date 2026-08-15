package com.fbrl.application.service;

import com.fbrl.application.port.in.RejectTransferUseCase;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.stereotype.Service;

@Service
public class RejectTransferService implements RejectTransferUseCase {

  private final LoadApprovalRequestPort loadApprovalRequestPort;
  private final SaveApprovalRequestPort saveApprovalRequestPort;

  public RejectTransferService(
      LoadApprovalRequestPort loadApprovalRequestPort,
      SaveApprovalRequestPort saveApprovalRequestPort) {
    this.loadApprovalRequestPort = loadApprovalRequestPort;
    this.saveApprovalRequestPort = saveApprovalRequestPort;
  }

  @Override
  public ApprovalDecisionResult reject(RejectTransferCommand command) {
    TransferApprovalRequest request =
        loadApprovalRequestPort
            .loadByRequestId(command.requestId())
            .orElseThrow(
                () ->
                    new ApprovalRequestNotFoundException(
                        "승인 요청을 찾을 수 없습니다. requestId: " + command.requestId()));

    request.reject(command.checkerId(), command.rejectionReason());
    TransferApprovalRequest saved = saveApprovalRequestPort.save(request);

    return new ApprovalDecisionResult(saved.getRequestId(), saved.getStatus());
  }
}
