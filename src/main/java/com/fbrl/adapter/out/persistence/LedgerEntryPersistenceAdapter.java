package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadLedgerEntriesPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.domain.exception.LedgerPersistenceException;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryPersistenceAdapter implements SaveLedgerEntryPort, LoadLedgerEntriesPort {

  private final LedgerEntryJpaRepository ledgerEntryJpaRepository;
  private final LedgerEntryMapper ledgerEntryMapper;

  public LedgerEntryPersistenceAdapter(
      LedgerEntryJpaRepository ledgerEntryJpaRepository, LedgerEntryMapper ledgerEntryMapper) {
    this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
    this.ledgerEntryMapper = ledgerEntryMapper;
  }

  @Override
  public List<LedgerEntry> saveAll(List<LedgerEntry> entries) {
    try {
      List<LedgerEntryJpaEntity> entities =
          entries.stream().map(ledgerEntryMapper::toEntity).toList();
      return ledgerEntryJpaRepository.saveAll(entities).stream()
          .map(ledgerEntryMapper::toDomain)
          .toList();
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("원장 저장 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public List<LedgerEntry> loadByAccountNumberSince(String accountNumber, Instant since) {
    try {
      return ledgerEntryJpaRepository
          .findByAccountNumberAndOccurredAtGreaterThanEqual(accountNumber, since)
          .stream()
          .map(ledgerEntryMapper::toDomain)
          .toList();
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("원장 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public Money sumAmountByDirection(LedgerDirection direction) {
    try {
      return Money.of(ledgerEntryJpaRepository.sumAmountByDirection(direction));
    } catch (DataAccessException e) {
      throw new LedgerPersistenceException("원장 합계 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  public void deleteAllInBatch() {
    ledgerEntryJpaRepository.deleteAllInBatch();
  }
}
