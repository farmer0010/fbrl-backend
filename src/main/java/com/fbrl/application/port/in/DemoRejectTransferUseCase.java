package com.fbrl.application.port.in;

import com.fbrl.application.port.in.RejectTransferUseCase.ApprovalDecisionResult;
import com.fbrl.application.port.in.RejectTransferUseCase.RejectTransferCommand;

public interface DemoRejectTransferUseCase {
  ApprovalDecisionResult reject(RejectTransferCommand command);
}
