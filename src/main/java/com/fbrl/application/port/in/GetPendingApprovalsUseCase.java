package com.fbrl.application.port.in;

import com.fbrl.domain.model.TransferApprovalRequest;
import java.util.List;

public interface GetPendingApprovalsUseCase {
  List<TransferApprovalRequest> getPendingApprovals();
}
