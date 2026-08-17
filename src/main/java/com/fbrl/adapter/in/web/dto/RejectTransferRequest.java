package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.RejectTransferUseCase.RejectTransferCommand;
import jakarta.validation.constraints.NotBlank;

public record RejectTransferRequest(@NotBlank(message = "거절 사유는 필수입니다.") String rejectionReason) {
  public RejectTransferCommand toCommand(String requestId, String checkerId) {
    return new RejectTransferCommand(requestId, checkerId, rejectionReason);
  }
}
