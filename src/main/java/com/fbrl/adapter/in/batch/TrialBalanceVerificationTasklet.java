package com.fbrl.adapter.in.batch;

import com.fbrl.application.port.in.VerifyTrialBalanceUseCase;
import com.fbrl.application.port.in.VerifyTrialBalanceUseCase.TrialBalanceVerificationResult;
import com.fbrl.domain.exception.TrialBalanceViolationException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

public class TrialBalanceVerificationTasklet implements Tasklet {

  private final VerifyTrialBalanceUseCase verifyTrialBalanceUseCase;

  public TrialBalanceVerificationTasklet(VerifyTrialBalanceUseCase verifyTrialBalanceUseCase) {
    this.verifyTrialBalanceUseCase = verifyTrialBalanceUseCase;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    TrialBalanceVerificationResult result = verifyTrialBalanceUseCase.verify();

    if (!result.balanced()) {
      throw new TrialBalanceViolationException(
          "대차평형이 깨졌습니다. 총 차변: " + result.totalDebit() + ", 총 대변: " + result.totalCredit());
    }

    return RepeatStatus.FINISHED;
  }
}
