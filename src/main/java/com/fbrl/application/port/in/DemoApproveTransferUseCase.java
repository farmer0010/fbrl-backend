package com.fbrl.application.port.in;

import com.fbrl.application.port.in.ApproveTransferUseCase.ApprovalDecisionResult;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;

public interface DemoApproveTransferUseCase {
  ApprovalDecisionResult approve(ApproveTransferCommand command);
}
