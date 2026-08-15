package com.fbrl.application.port.out;

import com.fbrl.domain.model.TransferApprovalRequest;

public interface SaveApprovalRequestPort {
  TransferApprovalRequest save(TransferApprovalRequest request);
}
