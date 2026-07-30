package com.fbrl.application.service;

import com.fbrl.adapter.out.persistence.AccountEntity;
import com.fbrl.adapter.out.persistence.AccountJpaRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LockComparisonService {

  private final AccountJpaRepository accountJpaRepository;

  public LockComparisonService(AccountJpaRepository accountJpaRepository) {
    this.accountJpaRepository = accountJpaRepository;
  }

  @Transactional
  public void transferWithPessimisticLock(String senderNo, String receiverNo, BigDecimal amount) {

    AccountEntity sender =
        accountJpaRepository.findByAccountNumberWithPessimisticLock(senderNo).orElseThrow();

    AccountEntity receiver =
        accountJpaRepository.findByAccountNumberWithPessimisticLock(receiverNo).orElseThrow();

    sender.setBalance(sender.getBalance().subtract(amount));
    receiver.setBalance(receiver.getBalance().add(amount));
  }

  @Transactional
  public void updateBalanceWithOptimisticLock(
      String senderNo, String receiverNo, BigDecimal amount) {

    AccountEntity sender =
        accountJpaRepository.findByAccountNumberWithOptimisticLock(senderNo).orElseThrow();

    AccountEntity receiver =
        accountJpaRepository.findByAccountNumberWithOptimisticLock(receiverNo).orElseThrow();

    sender.setBalance(sender.getBalance().subtract(amount));
    receiver.setBalance(receiver.getBalance().add(amount));
  }
}
