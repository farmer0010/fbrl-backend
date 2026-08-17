package com.fbrl.application.service;

import com.fbrl.application.port.in.SearchReconciliationDiscrepanciesUseCase;
import com.fbrl.application.port.out.LoadReconciliationDiscrepancyPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import org.springframework.stereotype.Service;

@Service
public class SearchReconciliationDiscrepanciesService
    implements SearchReconciliationDiscrepanciesUseCase {

  private final LoadReconciliationDiscrepancyPort loadReconciliationDiscrepancyPort;

  public SearchReconciliationDiscrepanciesService(
      LoadReconciliationDiscrepancyPort loadReconciliationDiscrepancyPort) {
    this.loadReconciliationDiscrepancyPort = loadReconciliationDiscrepancyPort;
  }

  @Override
  public PagedResult<ReconciliationDiscrepancy> search(
      SearchReconciliationDiscrepanciesQuery query) {
    return loadReconciliationDiscrepancyPort.search(
        query.status(), query.from(), query.to(), query.page(), query.size());
  }
}
