package com.fbrl.application.service;

import com.fbrl.application.port.out.SaveSagaPort;
import com.fbrl.domain.model.TransferSaga;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SagaStateWriter {

  private final SaveSagaPort saveSagaPort;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public TransferSaga save(TransferSaga saga) {
    return saveSagaPort.save(saga);
  }
}
