package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.application.port.out.LoadLatestEodSnapshotPort;
import com.fbrl.application.port.out.SaveEodSnapshotPort;
import com.fbrl.domain.exception.DuplicateEodSnapshotException;
import com.fbrl.domain.model.EodSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@Qualifier("demo")
public class DemoEodSnapshotPersistenceAdapter
    implements SaveEodSnapshotPort, LoadLatestEodSnapshotPort {

  private final DemoEodSnapshotJpaRepository demoEodSnapshotJpaRepository;
  private final DemoEodSnapshotMapper demoEodSnapshotMapper;

  public DemoEodSnapshotPersistenceAdapter(
      DemoEodSnapshotJpaRepository demoEodSnapshotJpaRepository,
      DemoEodSnapshotMapper demoEodSnapshotMapper) {
    this.demoEodSnapshotJpaRepository = demoEodSnapshotJpaRepository;
    this.demoEodSnapshotMapper = demoEodSnapshotMapper;
  }

  @Override
  public void saveAll(List<EodSnapshot> snapshots) {
    List<DemoEodSnapshotEntity> entities =
        snapshots.stream().map(demoEodSnapshotMapper::toEntity).toList();

    try {
      demoEodSnapshotJpaRepository.saveAll(entities);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateEodSnapshotException(
          "이미 정산이 완료된 데모 계좌·정산일자 조합이 청크에 포함되어 있습니다. 청크 크기: " + snapshots.size());
    }
  }

  @Override
  public Optional<EodSnapshot> findLatestByAccountNumber(String accountNumber) {
    return demoEodSnapshotJpaRepository
        .findTopByAccountNumberOrderByComputedAtDesc(accountNumber)
        .map(demoEodSnapshotMapper::toDomain);
  }

  public void deleteAllInBatch() {
    demoEodSnapshotJpaRepository.deleteAllInBatch();
  }
}
