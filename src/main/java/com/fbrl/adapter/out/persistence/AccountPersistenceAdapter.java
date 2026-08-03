package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.exception.DuplicateAccountNumberException;
import com.fbrl.domain.model.Account;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class AccountPersistenceAdapter implements AccountRepositoryPort {

  private final AccountJpaRepository accountJpaRepository;
  private final AccountMapper accountMapper;

  public AccountPersistenceAdapter(
      AccountJpaRepository accountJpaRepository, AccountMapper accountMapper) {
    this.accountJpaRepository = accountJpaRepository;
    this.accountMapper = accountMapper;
  }

  @Override
  public Optional<Account> findByAccountNumber(String accountNumber) {
    return accountJpaRepository.findByAccountNumber(accountNumber).map(accountMapper::toDomain);
  }

  @Override
  public Account save(Account account) {
    try {
      AccountEntity entity = accountMapper.toEntity(account);
      AccountEntity savedEntity = accountJpaRepository.save(entity);
      return accountMapper.toDomain(savedEntity);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateAccountNumberException(
          "이미 존재하는 계좌번호입니다. 계좌번호: " + account.getAccountNumber());
    }
  }
}
