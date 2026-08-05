package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements SaveOutboxEventPort {
  private final OutboxEventJpaRepository outboxEventJpaRepository;

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    OutboxEventJpaEntity entity = OutboxEventJpaEntity.fromDomain(outboxEvent);
    OutboxEventJpaEntity savedEntity = outboxEventJpaRepository.save(entity);
    return savedEntity.toDomain();
  }
}
