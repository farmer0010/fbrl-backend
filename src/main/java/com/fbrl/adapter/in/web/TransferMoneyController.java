package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.TransferMoneyRequest;
import com.fbrl.application.port.in.TransferMoneyCommand;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import com.fbrl.domain.exception.ApprovalRequiredException;
import com.fbrl.domain.model.ApprovalPolicy;
import com.fbrl.global.common.annotation.CheckIdempotency;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferMoneyController {
  private final TransferMoneyUseCase transferMoneyUseCase;
  private final ApprovalPolicy approvalPolicy;

  public TransferMoneyController(
      TransferMoneyUseCase transferMoneyUseCase, ApprovalPolicy approvalPolicy) {
    this.transferMoneyUseCase = transferMoneyUseCase;
    this.approvalPolicy = approvalPolicy;
  }

  @PostMapping
  @CheckIdempotency
  public ResponseEntity<Void> transfer(@Valid @RequestBody TransferMoneyRequest request) {
    TransferMoneyCommand command = request.toCommand();
    assertApprovalNotRequired(command);
    transferMoneyUseCase.transfer(command);
    return ResponseEntity.ok().build();
  }

  private void assertApprovalNotRequired(TransferMoneyCommand command) {
    if (approvalPolicy.requiresApproval(command.money())) {
      throw new ApprovalRequiredException(command.money().getAmount());
    }
  }
}
