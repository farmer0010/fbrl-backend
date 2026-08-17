package com.fbrl.application.service;

import com.fbrl.application.port.in.SearchTransferApprovalsUseCase;
import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.TransferApprovalRequest;
import org.springframework.stereotype.Service;

@Service
public class SearchTransferApprovalsService implements SearchTransferApprovalsUseCase {

  private final LoadApprovalRequestPort loadApprovalRequestPort;

  public SearchTransferApprovalsService(LoadApprovalRequestPort loadApprovalRequestPort) {
    this.loadApprovalRequestPort = loadApprovalRequestPort;
  }

  @Override
  public PagedResult<TransferApprovalRequest> search(SearchTransferApprovalsQuery query) {
    return loadApprovalRequestPort.search(
        query.status(), query.from(), query.to(), query.page(), query.size());
  }
}
