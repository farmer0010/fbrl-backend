package com.fbrl.application.port.out;

public interface DemoOutboxTamperPort {
  void tamperPayload(Long outboxEventId, String corruptedPayload);
}
