package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.application.port.out.LoadLedgerBalanceDeltasPort;
import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.exception.LedgerPersistenceException;
import com.fbrl.domain.model.LedgerBalanceDelta;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Qualifier("demo")
public class DemoLedgerEntryPersistenceAdapter
    implements SaveLedgerEntryPort, LoadLedgerEntriesPort, LoadLedgerBalanceDeltasPort {

  private final DemoLedgerEntryJpaRepository demoLedgerEntryJpaRepository;
  private final DemoLedgerEntryMapper demoLedgerEntryMapper;

  public DemoLedgerEntryPersistenceAdapter(
      DemoLedgerEntryJpaRepository demoLedgerEntryJpaRepository,
      DemoLedgerEntryMapper demoLedgerEntryMapper) {
    this.demoLedgerEntryJpaRepository = demoLedgerEntryJpaRepository;
    this.demoLedgerEntryMapper = demoLedgerEntryMapper;
  }

  @Override
  public List<LedgerEntry> saveAll(List<LedgerEntry> entries) {
    try {
      List<DemoLedgerEntryEntity> entities =
          entries.stream().map(demoLedgerEntryMapper::toEntity).toList();
      return demoLedgerEntryJpaRepository.saveAll(entities).stream()
          .map(demoLedgerEntryMapper::toDomain)
          .toList();
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("데모 원장 저장 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public List<LedgerEntry> loadByAccountNumberSince(String accountNumber, Instant since) {
    try {
      return demoLedgerEntryJpaRepository
          .findByAccountNumberAndOccurredAtGreaterThanEqual(accountNumber, since)
          .stream()
          .map(demoLedgerEntryMapper::toDomain)
          .toList();
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("데모 원장 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public PagedResult<LedgerEntry> loadByAccountNumberAndPeriod(
      String accountNumber, Instant from, Instant to, int page, int size) {
    try {
      Page<DemoLedgerEntryEntity> result =
          demoLedgerEntryJpaRepository.findByAccountNumberAndOccurredAtBetween(
              accountNumber, from, to, PageRequest.of(page, size));
      List<LedgerEntry> items =
          result.getContent().stream().map(demoLedgerEntryMapper::toDomain).toList();
      return new PagedResult<>(items, result.getTotalElements());
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("데모 원장 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public Money sumAmountByDirection(LedgerDirection direction) {
    try {
      return Money.of(demoLedgerEntryJpaRepository.sumAmountByDirection(direction));
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("데모 원장 합계 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public Map<String, LedgerBalanceDelta> loadBalanceDeltasUntil(
      List<String> accountNumbers, Instant until) {
    try {
      Map<String, LedgerBalanceDelta> deltasByAccountNumber = new HashMap<>();
      for (String accountNumber : accountNumbers) {
        deltasByAccountNumber.put(accountNumber, new LedgerBalanceDelta(Money.ZERO, Money.ZERO));
      }

      List<DemoLedgerBalanceDeltaProjection> rows =
          demoLedgerEntryJpaRepository.sumBalanceDeltasUntil(
              accountNumbers, until, LedgerDirection.CREDIT, LedgerDirection.DEBIT);
      for (DemoLedgerBalanceDeltaProjection row : rows) {
        deltasByAccountNumber.put(
            row.accountNumber(),
            new LedgerBalanceDelta(Money.of(row.creditTotal()), Money.of(row.debitTotal())));
      }

      return deltasByAccountNumber;
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("데모 계좌별 원장 델타 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  public void deleteAllInBatch() {
    demoLedgerEntryJpaRepository.deleteAllInBatch();
  }
}
