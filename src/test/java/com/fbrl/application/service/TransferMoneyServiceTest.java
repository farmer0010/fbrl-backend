package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.PayloadSerializerPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.Money;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferMoneyService 단위 테스트")
class TransferMoneyServiceTest {

  @Mock private AccountRepositoryPort accountRepositoryPort;
  @Mock private SaveOutboxEventPort saveOutboxEventPort;
  @Mock private PayloadSerializerPort payloadSerializerPort;

  @InjectMocks private TransferMoneyService transferMoneyService;

  @Test
  @DisplayName("정상적인 송금 요청 시 출금계좌 잔액은 줄어들고 입금계좌 잔액은 늘어난다.")
  void transferSuccess() {
    Account sender = Account.create("111-111", Money.wons(10000));
    Account receiver = Account.create("222-222", Money.wons(2000));
    TransferMoneyCommand command = new TransferMoneyCommand("111-111", "222-222", Money.wons(3000));

    given(accountRepositoryPort.findByAccountNumber("111-111")).willReturn(Optional.of(sender));
    given(accountRepositoryPort.findByAccountNumber("222-222")).willReturn(Optional.of(receiver));
    given(payloadSerializerPort.serialize(org.mockito.ArgumentMatchers.any()))
        .willReturn("{\"dummy\":\"payload\"}");

    transferMoneyService.transfer(command);

    assertThat(sender.getBalance()).isEqualTo(Money.wons(7000));
    assertThat(receiver.getBalance()).isEqualTo(Money.wons(5000));
    verify(accountRepositoryPort).save(sender);
    verify(accountRepositoryPort).save(receiver);
    verify(saveOutboxEventPort).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("출금 계좌가 존재하지 않으면 AccountNotFoundException이 발생한다.")
  void transferThrowsExceptionWhenSenderNotFound() {
    TransferMoneyCommand command = new TransferMoneyCommand("111-111", "222-222", Money.wons(3000));

    given(accountRepositoryPort.findByAccountNumber("111-111")).willReturn(Optional.empty());

    assertThatThrownBy(() -> transferMoneyService.transfer(command))
        .isInstanceOf(AccountNotFoundException.class)
        .hasMessageContaining("출금 계좌를 찾을 수 없습니다");
  }
}
