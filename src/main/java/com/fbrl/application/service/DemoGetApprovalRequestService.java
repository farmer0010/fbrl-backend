package com.fbrl.application.service;

import com.fbrl.application.port.in.DemoGetApprovalRequestUseCase;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class DemoGetApprovalRequestService implements DemoGetApprovalRequestUseCase {

  private final LoadApprovalRequestPort demoLoadApprovalRequestPort;

  public DemoGetApprovalRequestService(
      @Qualifier("demo") LoadApprovalRequestPort demoLoadApprovalRequestPort) {
    this.demoLoadApprovalRequestPort = demoLoadApprovalRequestPort;
  }

  @Override
  public TransferApprovalRequest getByRequestId(String requestId) {
    return demoLoadApprovalRequestPort
        .loadByRequestId(requestId)
        .orElseThrow(
            () ->
                new ApprovalRequestNotFoundException("승인 요청을 찾을 수 없습니다. requestId: " + requestId));
  }
}
