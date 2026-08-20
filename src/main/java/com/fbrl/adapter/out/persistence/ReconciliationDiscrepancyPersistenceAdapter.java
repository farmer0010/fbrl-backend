package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadReconciliationDiscrepancyPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.application.port.out.SaveReconciliationDiscrepancyPort;
import com.fbrl.domain.exception.DuplicateReconciliationDiscrepancyException;
import com.fbrl.domain.model.ReconciliationDiscrepancy;
import com.fbrl.domain.model.ReconciliationStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Primary
@Component
public class ReconciliationDiscrepancyPersistenceAdapter
    implements SaveReconciliationDiscrepancyPort, LoadReconciliationDiscrepancyPort {

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

  @Override
  public PagedResult<ReconciliationDiscrepancy> search(
      ReconciliationStatus status, LocalDate from, LocalDate to, int page, int size) {
    Page<ReconciliationDiscrepancyJpaEntity> result =
        reconciliationDiscrepancyJpaRepository.search(status, from, to, PageRequest.of(page, size));
    List<ReconciliationDiscrepancy> items =
        result.getContent().stream().map(reconciliationDiscrepancyMapper::toDomain).toList();
    return new PagedResult<>(items, result.getTotalElements());
  }

  public void deleteAllInBatch() {
    reconciliationDiscrepancyJpaRepository.deleteAllInBatch();
  }
}
