package com.fbrl.application.port.in;

import com.fbrl.domain.model.TransferApprovalRequest;

public interface DemoGetApprovalRequestUseCase {
  TransferApprovalRequest getByRequestId(String requestId);
}
