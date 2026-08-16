package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadEodSnapshotByDatePort;
import com.fbrl.application.port.out.LoadLatestEodSnapshotPort;
import com.fbrl.application.port.out.SaveEodSnapshotPort;
import com.fbrl.domain.exception.DuplicateEodSnapshotException;
import com.fbrl.domain.exception.EodSnapshotPersistenceException;
import com.fbrl.domain.model.EodSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class EodSnapshotPersistenceAdapter
    implements SaveEodSnapshotPort, LoadLatestEodSnapshotPort, LoadEodSnapshotByDatePort {

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

  @Override
  public Map<String, EodSnapshot> loadByAccountNumbersAndDate(
      List<String> accountNumbers, LocalDate date) {
    try {
      return eodSnapshotJpaRepository
          .findByAccountNumberInAndSettlementDate(accountNumbers, date)
          .stream()
          .map(eodSnapshotMapper::toDomain)
          .collect(Collectors.toMap(EodSnapshot::accountNumber, Function.identity()));
    } catch (DataAccessException e) {
      throw new EodSnapshotPersistenceException("EOD 스냅샷 배치 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  public void deleteAllInBatch() {
    eodSnapshotJpaRepository.deleteAllInBatch();
  }
}
