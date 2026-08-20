package com.fbrl.application.service;

import com.fbrl.application.port.in.DemoRequestTransferApprovalUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.RequestTransferApprovalCommand;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalNotRequiredException;
import com.fbrl.domain.model.ApprovalPolicy;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemoRequestTransferApprovalService implements DemoRequestTransferApprovalUseCase {

  private final ApprovalPolicy approvalPolicy;
  private final SaveApprovalRequestPort demoSaveApprovalRequestPort;

  public DemoRequestTransferApprovalService(
      ApprovalPolicy approvalPolicy,
      @Qualifier("demo") SaveApprovalRequestPort demoSaveApprovalRequestPort) {
    this.approvalPolicy = approvalPolicy;
    this.demoSaveApprovalRequestPort = demoSaveApprovalRequestPort;
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
    TransferApprovalRequest saved = demoSaveApprovalRequestPort.save(request);

    return new ApprovalRequestResult(saved.getRequestId(), saved.getStatus());
  }
}
