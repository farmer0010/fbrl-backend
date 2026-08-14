package com.fbrl.application.port.in;

import com.fbrl.domain.event.TransferCompletedEvent;

public interface ProcessTransferEventUseCase {
  void handle(TransferCompletedEvent event);
}
