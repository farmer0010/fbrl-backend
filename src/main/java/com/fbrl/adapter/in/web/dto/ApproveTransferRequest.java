package com.fbrl.adapter.in.web.dto;

import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import jakarta.validation.constraints.NotBlank;

public record ApproveTransferRequest(@NotBlank(message = "승인자 ID는 필수입니다.") String checkerId) {
  public ApproveTransferCommand toCommand(String requestId) {
    return new ApproveTransferCommand(requestId, checkerId);
  }
}
