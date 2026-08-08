package com.fbrl.application.port.out;

import com.fbrl.domain.model.OutboxEvent;

public interface EventPublisherPort {
  void publish(OutboxEvent event);
}
