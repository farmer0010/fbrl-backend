package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.SaveReconciliationDiscrepancyPort;
import com.fbrl.domain.exception.DuplicateReconciliationDiscrepancyException;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ReconciliationDiscrepancyPersistenceAdapter
    implements SaveReconciliationDiscrepancyPort {

  private final ReconciliationDiscrepancyJpaRepository reconciliationDiscrepancyJpaRepository;
  private final ReconciliationDiscrepancyMapper reconciliationDiscrepancyMapper;

  public ReconciliationDiscrepancyPersistenceAdapter(
      ReconciliationDiscrepancyJpaRepository reconciliationDiscrepancyJpaRepository,
      ReconciliationDiscrepancyMapper reconciliationDiscrepancyMapper) {
    this.reconciliationDiscrepancyJpaRepository = reconciliationDiscrepancyJpaRepository;
    this.reconciliationDiscrepancyMapper = reconciliationDiscrepancyMapper;
  }

  @Override
  public void saveAll(List<ReconciliationDiscrepancy> discrepancies) {
    List<ReconciliationDiscrepancyJpaEntity> entities =
        discrepancies.stream().map(reconciliationDiscrepancyMapper::toEntity).toList();

    try {
      reconciliationDiscrepancyJpaRepository.saveAll(entities);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateReconciliationDiscrepancyException(
          "이미 대사가 완료된 계좌·정산일자 조합이 청크에 포함되어 있습니다. 청크 크기: " + discrepancies.size());
    }
  }

  public void deleteAllInBatch() {
    reconciliationDiscrepancyJpaRepository.deleteAllInBatch();
  }
}
