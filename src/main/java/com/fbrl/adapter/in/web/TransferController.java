package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.TransferRequest;
import com.fbrl.application.port.in.TransferMoneyUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
  private final TransferMoneyUseCase transferMoneyUseCase;

  public TransferController(TransferMoneyUseCase transferMoneyUseCase) {
    this.transferMoneyUseCase = transferMoneyUseCase;
  }

  @PostMapping
  public ResponseEntity<Void> transfer(@Valid @RequestBody TransferRequest request) {
    transferMoneyUseCase.transfer(request.toCommand());
    return ResponseEntity.ok().build();
  }
}
