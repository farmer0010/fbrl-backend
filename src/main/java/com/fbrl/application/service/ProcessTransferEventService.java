// src/main/java/com/fbrl/application/service/ProcessTransferEventService.java
package com.fbrl.application.service;

import com.fbrl.application.port.in.ProcessTransferEventUseCase;
import com.fbrl.domain.event.TransferCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessTransferEventService implements ProcessTransferEventUseCase {

  private static final Logger log = LoggerFactory.getLogger(ProcessTransferEventService.class);

  @Override
  public void handle(TransferCompletedEvent event) {
    log.info(
        "송금 완료 이벤트 처리 - sender={}, receiver={}, amount={}, occurredAt={}",
        event.senderAccountNumber(),
        event.receiverAccountNumber(),
        event.amount(),
        event.occurredAt());
  }
}
