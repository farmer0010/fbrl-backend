package com.fbrl.domain.exception;

import java.math.BigDecimal;

public class ApprovalRequiredException extends RuntimeException {
  public ApprovalRequiredException(BigDecimal amount) {
    super(
        "승인 임계 금액 이상이라 직접 이체할 수 없습니다. 요청된 금액: "
            + amount
            + " (해당 금액은 /api/v1/transfer-approvals로 승인 요청하세요.)");
  }
}
