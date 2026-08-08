package com.fbrl.application.port.out;

import com.fbrl.domain.model.TransferSaga;

public interface SaveSagaPort {
  TransferSaga save(TransferSaga saga);
}
