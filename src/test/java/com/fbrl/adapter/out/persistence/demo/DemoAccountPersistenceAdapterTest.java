package com.fbrl.adapter.out.persistence.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.adapter.out.persistence.AccountPersistenceAdapter;
import com.fbrl.domain.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("데모 계좌 저장이 운영 DB와 완전히 격리되는지 검증")
class DemoAccountPersistenceAdapterTest {

  @Autowired private DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;

  @Autowired private AccountPersistenceAdapter accountPersistenceAdapter;

  @BeforeEach
  void setUp() {
    demoAccountPersistenceAdapter.deleteAllInBatch();
    accountPersistenceAdapter.deleteAllInBatch();
  }

  @Test
  @DisplayName("데모 DB에 저장된 계좌는 데모 어댑터로 조회되고, 운영 어댑터로는 조회되지 않는다")
  void savedDemoAccount_isIsolatedFromMainDatabase() {
    String accountNumber = "DEMO-ISOLATION-TEST";

    demoAccountPersistenceAdapter.save(Account.create(accountNumber));

    assertThat(demoAccountPersistenceAdapter.findByAccountNumber(accountNumber)).isPresent();
    assertThat(accountPersistenceAdapter.findByAccountNumber(accountNumber)).isEmpty();
  }

  @Test
  @DisplayName("운영 DB에 저장된 계좌는 운영 어댑터로 조회되고, 데모 어댑터로는 조회되지 않는다")
  void savedMainAccount_isIsolatedFromDemoDatabase() {
    String accountNumber = "MAIN-ISOLATION-TEST";

    accountPersistenceAdapter.save(Account.create(accountNumber));

    assertThat(accountPersistenceAdapter.findByAccountNumber(accountNumber)).isPresent();
    assertThat(demoAccountPersistenceAdapter.findByAccountNumber(accountNumber)).isEmpty();
  }
}
