package com.fbrl.adapter.in.web.demo;

import com.fbrl.adapter.in.web.dto.TransferMoneyRequest;
import com.fbrl.application.port.in.DemoTransferMoneyUseCase;
import com.fbrl.application.port.in.TransferMoneyCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/transfers")
public class DemoTransferController {
  private final DemoTransferMoneyUseCase demoTransferMoneyUseCase;

  public DemoTransferController(DemoTransferMoneyUseCase demoTransferMoneyUseCase) {
    this.demoTransferMoneyUseCase = demoTransferMoneyUseCase;
  }

  @PostMapping
  public ResponseEntity<Void> transfer(@Valid @RequestBody TransferMoneyRequest request) {
    TransferMoneyCommand command = request.toCommand();
    demoTransferMoneyUseCase.transfer(command);
    return ResponseEntity.ok().build();
  }
}
