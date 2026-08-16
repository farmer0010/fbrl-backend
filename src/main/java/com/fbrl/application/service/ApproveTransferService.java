package com.fbrl.application.service;

import com.fbrl.application.port.in.ApproveTransferUseCase;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApproveTransferService implements ApproveTransferUseCase {

  private static final Logger log = LoggerFactory.getLogger(ApproveTransferService.class);

  private final LoadApprovalRequestPort loadApprovalRequestPort;
  private final SaveApprovalRequestPort saveApprovalRequestPort;
  private final TransferMoneyUseCase transferMoneyUseCase;

  public ApproveTransferService(
      LoadApprovalRequestPort loadApprovalRequestPort,
      SaveApprovalRequestPort saveApprovalRequestPort,
      TransferMoneyUseCase transferMoneyUseCase) {
    this.loadApprovalRequestPort = loadApprovalRequestPort;
    this.saveApprovalRequestPort = saveApprovalRequestPort;
    this.transferMoneyUseCase = transferMoneyUseCase;
  }

  @Override
  public ApprovalDecisionResult approve(ApproveTransferCommand command) {
    TransferApprovalRequest request =
        loadApprovalRequestPort
            .loadByRequestId(command.requestId())
            .orElseThrow(
                () ->
                    new ApprovalRequestNotFoundException(
                        "승인 요청을 찾을 수 없습니다. requestId: " + command.requestId()));

    request.approve(command.checkerId());
    TransferApprovalRequest saved = saveApprovalRequestPort.save(request);

    try {
      transferMoneyUseCase.transfer(
          new TransferMoneyCommand(
              saved.getFromAccountNumber(), saved.getToAccountNumber(), saved.getAmount()));
    } catch (RuntimeException e) {
      saved.markExecutionFailed(e.getMessage());
      trySaveExecutionResult(saved);
      throw e;
    }

    saved.markExecuted();
    saved = trySaveExecutionResult(saved);

    return new ApprovalDecisionResult(saved.getRequestId(), saved.getStatus());
  }

  private TransferApprovalRequest trySaveExecutionResult(TransferApprovalRequest request) {
    try {
      return saveApprovalRequestPort.save(request);
    } catch (RuntimeException e) {
      log.error("승인 요청 {}의 집행 결과(executionStatus) 저장에 실패했습니다.", request.getRequestId(), e);
      return request;
    }
  }
}
