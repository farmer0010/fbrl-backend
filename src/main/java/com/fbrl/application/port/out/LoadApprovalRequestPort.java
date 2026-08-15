package com.fbrl.application.port.out;

import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.util.List;
import java.util.Optional;

public interface LoadApprovalRequestPort {
  Optional<TransferApprovalRequest> loadByRequestId(String requestId);

  List<TransferApprovalRequest> loadByStatus(ApprovalStatus status);
}
