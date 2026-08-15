package com.fbrl.application.service;

import com.fbrl.application.port.in.RequestTransferApprovalUseCase;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalNotRequiredException;
import com.fbrl.domain.model.ApprovalPolicy;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestTransferApprovalService implements RequestTransferApprovalUseCase {

  private final ApprovalPolicy approvalPolicy;
  private final SaveApprovalRequestPort saveApprovalRequestPort;

  public RequestTransferApprovalService(
      ApprovalPolicy approvalPolicy, SaveApprovalRequestPort saveApprovalRequestPort) {
    this.approvalPolicy = approvalPolicy;
    this.saveApprovalRequestPort = saveApprovalRequestPort;
  }

  @Override
  public ApprovalRequestResult requestApproval(RequestTransferApprovalCommand command) {
    if (!approvalPolicy.requiresApproval(command.amount())) {
      throw new ApprovalNotRequiredException(command.amount().getAmount());
    }

    TransferApprovalRequest request =
        TransferApprovalRequest.request(
            command.makerId(),
            command.fromAccountNumber(),
            command.toAccountNumber(),
            command.amount());
    TransferApprovalRequest saved = saveApprovalRequestPort.save(request);

    return new ApprovalRequestResult(saved.getRequestId(), saved.getStatus());
  }
}
