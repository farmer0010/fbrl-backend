package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.RejectTransferUseCase.RejectTransferCommand;
import jakarta.validation.constraints.NotBlank;

public record RejectTransferRequest(
    @NotBlank(message = "승인자 ID는 필수입니다.") String checkerId,
    @NotBlank(message = "거절 사유는 필수입니다.") String rejectionReason) {
  public RejectTransferCommand toCommand(String requestId) {
    return new RejectTransferCommand(requestId, checkerId, rejectionReason);
  }
}
