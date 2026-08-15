package com.fbrl.application.port.in;

import com.fbrl.domain.model.TransferApprovalRequest;

public interface GetApprovalRequestUseCase {
  TransferApprovalRequest getByRequestId(String requestId);
}
