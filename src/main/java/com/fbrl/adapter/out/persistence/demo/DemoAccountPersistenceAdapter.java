package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.LoadAllAccountsPort;
import com.fbrl.domain.exception.AccountPersistenceException;
import com.fbrl.domain.exception.DuplicateAccountNumberException;
import com.fbrl.domain.model.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Qualifier("demo")
public class DemoAccountPersistenceAdapter implements AccountRepositoryPort, LoadAllAccountsPort {

  private final DemoAccountJpaRepository demoAccountJpaRepository;
  private final DemoAccountMapper demoAccountMapper;

  public DemoAccountPersistenceAdapter(
      DemoAccountJpaRepository demoAccountJpaRepository, DemoAccountMapper demoAccountMapper) {
    this.demoAccountJpaRepository = demoAccountJpaRepository;
    this.demoAccountMapper = demoAccountMapper;
  }

  @Override
  public Optional<Account> findByAccountNumber(String accountNumber) {
    try {
      return demoAccountJpaRepository
          .findByAccountNumber(accountNumber)
          .map(demoAccountMapper::toDomain);
    } catch (DataAccessException e) {
      throw new AccountPersistenceException("데모 계좌 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public Account save(Account account) {
    try {
      DemoAccountEntity entity = demoAccountMapper.toEntity(account);
      DemoAccountEntity savedEntity = demoAccountJpaRepository.save(entity);
      return demoAccountMapper.toDomain(savedEntity);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateAccountNumberException(
          "이미 존재하는 데모 계좌번호입니다. 계좌번호: " + account.getAccountNumber());
    }
  }

  @Override
  public List<Account> loadAccounts(int page, int size) {
    try {
      Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
      return demoAccountJpaRepository
          .findAll(pageable)
          .map(demoAccountMapper::toDomain)
          .getContent();
    } catch (DataAccessException e) {
      throw new AccountPersistenceException("데모 계좌 페이징 조회 중 인프라 예외가 발생했습니다.", e);
    }
  }

  public void deleteAllInBatch() {
    demoAccountJpaRepository.deleteAllInBatch();
  }
}
