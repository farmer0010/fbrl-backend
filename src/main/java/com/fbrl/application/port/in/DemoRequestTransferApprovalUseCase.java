package com.fbrl.application.port.in;

import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.RequestTransferApprovalCommand;

public interface DemoRequestTransferApprovalUseCase {
  ApprovalRequestResult requestApproval(RequestTransferApprovalCommand command);
}
