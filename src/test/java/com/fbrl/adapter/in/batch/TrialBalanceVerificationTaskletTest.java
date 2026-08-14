package com.fbrl.adapter.in.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.in.VerifyTrialBalanceUseCase.TrialBalanceVerificationResult;
import com.fbrl.domain.exception.TrialBalanceViolationException;
import com.fbrl.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrialBalanceVerificationTasklet 단위 테스트")
class TrialBalanceVerificationTaskletTest {

  @Mock private VerifyTrialBalanceUseCase verifyTrialBalanceUseCase;

  @Test
  @DisplayName("대차평형이 유지되면 FINISHED를 반환한다.")
  void execute_balanced_returnsFinished() {
    given(verifyTrialBalanceUseCase.verify())
        .willReturn(TrialBalanceVerificationResult.of(Money.wons(1000), Money.wons(1000)));

    TrialBalanceVerificationTasklet tasklet =
        new TrialBalanceVerificationTasklet(verifyTrialBalanceUseCase);

    RepeatStatus status = tasklet.execute(null, null);

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
  }

  @Test
  @DisplayName("대차평형이 깨지면 TrialBalanceViolationException을 던져 스텝을 실패시킨다.")
  void execute_unbalanced_throwsException() {
    given(verifyTrialBalanceUseCase.verify())
        .willReturn(TrialBalanceVerificationResult.of(Money.wons(1000), Money.wons(900)));

    TrialBalanceVerificationTasklet tasklet =
        new TrialBalanceVerificationTasklet(verifyTrialBalanceUseCase);

    assertThatThrownBy(() -> tasklet.execute(null, null))
        .isInstanceOf(TrialBalanceViolationException.class);
  }
}
