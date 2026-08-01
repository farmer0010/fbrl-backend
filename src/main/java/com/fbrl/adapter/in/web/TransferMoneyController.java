package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.TransferMoneyRequest;
import com.fbrl.application.port.in.TransferMoneyUseCase;
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

  public TransferMoneyController(TransferMoneyUseCase transferMoneyUseCase) {
    this.transferMoneyUseCase = transferMoneyUseCase;
  }

  @PostMapping
  @CheckIdempotency
  public ResponseEntity<Void> transfer(@Valid @RequestBody TransferMoneyRequest request) {
    transferMoneyUseCase.transfer(request.toCommand());
    return ResponseEntity.ok().build();
  }
}
