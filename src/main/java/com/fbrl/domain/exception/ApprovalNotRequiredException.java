package com.fbrl.domain.exception;

import java.math.BigDecimal;

public class ApprovalNotRequiredException extends RuntimeException {
  public ApprovalNotRequiredException(BigDecimal amount) {
    super(
        "승인 임계 금액 미만이라 승인 절차가 필요하지 않습니다. 요청된 금액: "
            + amount
            + " (해당 금액은 /api/v1/transfers로 바로 이체하세요.)");
  }
}
