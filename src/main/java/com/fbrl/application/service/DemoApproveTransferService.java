package com.fbrl.application.service;

import com.fbrl.application.port.in.ApproveTransferUseCase.ApprovalDecisionResult;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import com.fbrl.application.port.in.DemoApproveTransferUseCase;
import com.fbrl.application.port.in.DemoTransferMoneyUseCase;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemoApproveTransferService implements DemoApproveTransferUseCase {

  private static final Logger log = LoggerFactory.getLogger(DemoApproveTransferService.class);

  private final LoadApprovalRequestPort demoLoadApprovalRequestPort;
  private final SaveApprovalRequestPort demoSaveApprovalRequestPort;
  private final DemoTransferMoneyUseCase demoTransferMoneyUseCase;

  public DemoApproveTransferService(
      @Qualifier("demo") LoadApprovalRequestPort demoLoadApprovalRequestPort,
      @Qualifier("demo") SaveApprovalRequestPort demoSaveApprovalRequestPort,
      DemoTransferMoneyUseCase demoTransferMoneyUseCase) {
    this.demoLoadApprovalRequestPort = demoLoadApprovalRequestPort;
    this.demoSaveApprovalRequestPort = demoSaveApprovalRequestPort;
    this.demoTransferMoneyUseCase = demoTransferMoneyUseCase;
  }

  @Override
  public ApprovalDecisionResult approve(ApproveTransferCommand command) {
    TransferApprovalRequest request =
        demoLoadApprovalRequestPort
            .loadByRequestId(command.requestId())
            .orElseThrow(
                () ->
                    new ApprovalRequestNotFoundException(
                        "승인 요청을 찾을 수 없습니다. requestId: " + command.requestId()));

    request.approve(command.checkerId());
    TransferApprovalRequest saved = demoSaveApprovalRequestPort.save(request);

    try {
      demoTransferMoneyUseCase.transfer(
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
      return demoSaveApprovalRequestPort.save(request);
    } catch (RuntimeException e) {
      log.error("데모 승인 요청 {}의 집행 결과(executionStatus) 저장에 실패했습니다.", request.getRequestId(), e);
      return request;
    }
  }
}
