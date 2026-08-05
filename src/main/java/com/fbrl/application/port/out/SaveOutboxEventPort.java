package com.fbrl.application.port.out;

import com.fbrl.domain.model.OutboxEvent;

public interface SaveOutboxEventPort {
  OutboxEvent save(OutboxEvent event);
}
