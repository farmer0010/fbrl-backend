package com.fbrl.application.service;

import com.fbrl.application.port.in.GetApprovalRequestUseCase;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalRequestNotFoundException;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.stereotype.Service;

@Service
public class GetApprovalRequestService implements GetApprovalRequestUseCase {

  private final LoadApprovalRequestPort loadApprovalRequestPort;

  public GetApprovalRequestService(LoadApprovalRequestPort loadApprovalRequestPort) {
    this.loadApprovalRequestPort = loadApprovalRequestPort;
  }

  @Override
  public TransferApprovalRequest getByRequestId(String requestId) {
    return loadApprovalRequestPort
        .loadByRequestId(requestId)
        .orElseThrow(
            () ->
                new ApprovalRequestNotFoundException("승인 요청을 찾을 수 없습니다. requestId: " + requestId));
  }
}
