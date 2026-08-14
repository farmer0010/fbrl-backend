package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadLatestEodSnapshotPort;
import com.fbrl.application.port.out.SaveEodSnapshotPort;
import com.fbrl.domain.exception.DuplicateEodSnapshotException;
import com.fbrl.domain.model.EodSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class EodSnapshotPersistenceAdapter
    implements SaveEodSnapshotPort, LoadLatestEodSnapshotPort {

  private final EodSnapshotJpaRepository eodSnapshotJpaRepository;
  private final EodSnapshotMapper eodSnapshotMapper;

  public EodSnapshotPersistenceAdapter(
      EodSnapshotJpaRepository eodSnapshotJpaRepository, EodSnapshotMapper eodSnapshotMapper) {
    this.eodSnapshotJpaRepository = eodSnapshotJpaRepository;
    this.eodSnapshotMapper = eodSnapshotMapper;
  }

  @Override
  public void saveAll(List<EodSnapshot> snapshots) {
    List<EodSnapshotJpaEntity> entities =
        snapshots.stream().map(eodSnapshotMapper::toEntity).toList();

    try {
      eodSnapshotJpaRepository.saveAll(entities);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateEodSnapshotException(
          "이미 정산이 완료된 계좌·정산일자 조합이 청크에 포함되어 있습니다. 청크 크기: " + snapshots.size());
    }
  }

  @Override
  public Optional<EodSnapshot> findLatestByAccountNumber(String accountNumber) {
    return eodSnapshotJpaRepository
        .findTopByAccountNumberOrderByComputedAtDesc(accountNumber)
        .map(eodSnapshotMapper::toDomain);
  }

  public void deleteAllInBatch() {
    eodSnapshotJpaRepository.deleteAllInBatch();
  }
}
