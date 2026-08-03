package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAccountService 단위 테스트")
class GetAccountServiceTest {

  @Mock private AccountRepositoryPort accountRepositoryPort;
  @InjectMocks private GetAccountService getAccountService;

  @Test
  @DisplayName("존재하는 계좌번호로 조회하면 계좌 정보를 반환한다.")
  void getAccountSuccess() {
    Account account = Account.open("110-0001-0001");
    given(accountRepositoryPort.findByAccountNumber("110-0001-0001"))
        .willReturn(Optional.of(account));

    Account result = getAccountService.getAccount("110-0001-0001");

    assertThat(result.getAccountNumber()).isEqualTo("110-0001-0001");
  }

  @Test
  @DisplayName("존재하지 않는 계좌번호로 조회하면 AccountNotFoundException이 발생한다.")
  void getAccountNotFound() {
    given(accountRepositoryPort.findByAccountNumber("999-9999-9999")).willReturn(Optional.empty());

    assertThatThrownBy(() -> getAccountService.getAccount("999-9999-9999"))
        .isInstanceOf(AccountNotFoundException.class);
  }
}
