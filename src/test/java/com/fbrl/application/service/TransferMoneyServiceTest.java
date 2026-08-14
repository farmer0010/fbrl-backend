package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.out.AccountRepositoryPort;
import com.fbrl.application.port.out.PayloadSerializerPort;
import com.fbrl.application.port.out.SaveLedgerEntryPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.exception.InsufficientBalanceException;
import com.fbrl.domain.exception.ReservedAccountException;
import com.fbrl.domain.model.Account;
import com.fbrl.domain.model.LedgerDirection;
import com.fbrl.domain.model.LedgerEntry;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.SystemAccounts;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferMoneyService 단위 테스트")
class TransferMoneyServiceTest {

  @Mock private AccountRepositoryPort accountRepositoryPort;
  @Mock private AccountBalanceCalculator accountBalanceCalculator;
  @Mock private SaveLedgerEntryPort saveLedgerEntryPort;
  @Mock private SaveOutboxEventPort saveOutboxEventPort;
  @Mock private PayloadSerializerPort payloadSerializerPort;

  @InjectMocks private TransferMoneyService transferMoneyService;

  @Test
  @DisplayName("정상적인 송금 요청 시 잔액이 충분하면 DEBIT/CREDIT 원장 쌍이 append-only로 저장된다.")
  void transferSuccess() {
    Account sender = Account.open("111-111");
    Account receiver = Account.open("222-222");
    TransferMoneyCommand command = new TransferMoneyCommand("111-111", "222-222", Money.wons(3000));

    given(accountRepositoryPort.findByAccountNumber("111-111")).willReturn(Optional.of(sender));
    given(accountRepositoryPort.findByAccountNumber("222-222")).willReturn(Optional.of(receiver));
    given(accountBalanceCalculator.calculate(sender)).willReturn(Money.wons(10000));
    given(payloadSerializerPort.serialize(any())).willReturn("{\"dummy\":\"payload\"}");

    transferMoneyService.transfer(command);

    ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
    verify(saveLedgerEntryPort).saveAll(captor.capture());

    List<LedgerEntry> savedEntries = captor.getValue();
    assertThat(savedEntries).hasSize(2);
    assertThat(savedEntries)
        .anySatisfy(
            entry -> {
              assertThat(entry.accountNumber()).isEqualTo("111-111");
              assertThat(entry.direction()).isEqualTo(LedgerDirection.DEBIT);
              assertThat(entry.amount()).isEqualTo(Money.wons(3000));
            });
    assertThat(savedEntries)
        .anySatisfy(
            entry -> {
              assertThat(entry.accountNumber()).isEqualTo("222-222");
              assertThat(entry.direction()).isEqualTo(LedgerDirection.CREDIT);
              assertThat(entry.amount()).isEqualTo(Money.wons(3000));
            });

    verify(saveOutboxEventPort).save(any());
  }

  @Test
  @DisplayName("출금 계좌의 계산된 잔액이 송금액보다 부족하면 InsufficientBalanceException이 발생하고 원장이 저장되지 않는다.")
  void transferThrowsExceptionWhenInsufficientBalance() {
    Account sender = Account.open("111-111");
    Account receiver = Account.open("222-222");
    TransferMoneyCommand command = new TransferMoneyCommand("111-111", "222-222", Money.wons(3000));

    given(accountRepositoryPort.findByAccountNumber("111-111")).willReturn(Optional.of(sender));
    given(accountRepositoryPort.findByAccountNumber("222-222")).willReturn(Optional.of(receiver));
    given(accountBalanceCalculator.calculate(sender)).willReturn(Money.wons(1000));

    assertThatThrownBy(() -> transferMoneyService.transfer(command))
        .isInstanceOf(InsufficientBalanceException.class);

    verify(saveLedgerEntryPort, org.mockito.Mockito.never())
        .saveAll(org.mockito.ArgumentMatchers.anyList());
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

  @Test
  @DisplayName("송금 계좌가 시스템 예약 계좌이면 어떤 포트도 건드리지 않고 ReservedAccountException이 발생한다.")
  void transferThrowsExceptionWhenSenderIsReservedAccount() {
    TransferMoneyCommand command =
        new TransferMoneyCommand(
            SystemAccounts.OPENING_BALANCE_SOURCE, "222-222", Money.wons(3000));

    assertThatThrownBy(() -> transferMoneyService.transfer(command))
        .isInstanceOf(ReservedAccountException.class);

    verifyNoInteractions(
        accountRepositoryPort, accountBalanceCalculator, saveLedgerEntryPort, saveOutboxEventPort);
  }

  @Test
  @DisplayName("수취 계좌가 시스템 예약 계좌이면 어떤 포트도 건드리지 않고 ReservedAccountException이 발생한다.")
  void transferThrowsExceptionWhenReceiverIsReservedAccount() {
    TransferMoneyCommand command =
        new TransferMoneyCommand(
            "111-111", SystemAccounts.OPENING_BALANCE_SOURCE, Money.wons(3000));

    assertThatThrownBy(() -> transferMoneyService.transfer(command))
        .isInstanceOf(ReservedAccountException.class);

    verifyNoInteractions(
        accountRepositoryPort, accountBalanceCalculator, saveLedgerEntryPort, saveOutboxEventPort);
  }
}
