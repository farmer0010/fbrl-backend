package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fbrl.domain.exception.DuplicateAccountNumberException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountService 단위 테스트")
class CreateAccountServiceTest {

  @Mock private AccountNumberPolicy accountNumberPolicy;
  @Mock private AccountCreationExecutor accountCreationExecutor;

  @Test
  @DisplayName("계좌 생성 성공 시 잔액 0원인 계좌를 반환한다.")
  void createAccountSuccess() {
    given(accountNumberPolicy.generate()).willReturn("110-0001-0001");
    given(accountCreationExecutor.createInNewTransaction("110-0001-0001"))
        .willReturn(Account.open("110-0001-0001"));

    CreateAccountService sut =
        new CreateAccountService(accountNumberPolicy, accountCreationExecutor);
    Account result = sut.createAccount();

    assertThat(result.getBalance()).isEqualTo(Money.ZERO);
  }

  @Test
  @DisplayName("첫 채번이 중복이면 재시도하여 두 번째 시도에서 성공한다.")
  void retriesOnDuplicateThenSucceeds() {
    given(accountNumberPolicy.generate()).willReturn("110-0001-0001", "110-0002-0002");
    given(accountCreationExecutor.createInNewTransaction("110-0001-0001"))
        .willThrow(new DuplicateAccountNumberException("중복"));
    given(accountCreationExecutor.createInNewTransaction("110-0002-0002"))
        .willReturn(Account.open("110-0002-0002"));

    CreateAccountService sut =
        new CreateAccountService(accountNumberPolicy, accountCreationExecutor);
    Account result = sut.createAccount();

    assertThat(result.getAccountNumber()).isEqualTo("110-0002-0002");
    verify(accountCreationExecutor, times(2)).createInNewTransaction(anyString());
  }

  @Test
  @DisplayName("MAX_RETRY만큼 계속 중복되면 DuplicateAccountNumberException을 던진다.")
  void throwsAfterMaxRetry() {
    given(accountNumberPolicy.generate()).willReturn("110-0001-0001");
    given(accountCreationExecutor.createInNewTransaction("110-0001-0001"))
        .willThrow(new DuplicateAccountNumberException("중복"));

    CreateAccountService sut =
        new CreateAccountService(accountNumberPolicy, accountCreationExecutor);

    assertThatThrownBy(sut::createAccount).isInstanceOf(DuplicateAccountNumberException.class);
    verify(accountCreationExecutor, times(3)).createInNewTransaction("110-0001-0001");
  }
}
