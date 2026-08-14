package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadAllOutboxEventsPort;
import com.fbrl.application.port.out.SaveOutboxEventPort;
import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPersistenceAdapter implements SaveOutboxEventPort, LoadAllOutboxEventsPort {
  private final OutboxEventJpaRepository outboxEventJpaRepository;
  private final OutboxChainTailJpaRepository outboxChainTailJpaRepository;

  @Override
  public OutboxEvent save(OutboxEvent outboxEvent) {
    outboxChainTailJpaRepository.ensureInitialized(OutboxEvent.GENESIS_PREVIOUS_HASH);
    OutboxChainTailJpaEntity tail =
        outboxChainTailJpaRepository
            .findForUpdate()
            .orElseThrow(() -> new IllegalStateException("outbox_chain_tail 초기화에 실패했습니다."));

    OutboxEvent chainedEvent = outboxEvent.chainedWith(tail.getLatestEntryHash());

    OutboxEventJpaEntity entity = OutboxEventJpaEntity.fromDomain(chainedEvent);
    OutboxEventJpaEntity savedEntity = outboxEventJpaRepository.save(entity);

    tail.updateLatestEntryHash(savedEntity.getEntryHash());

    return savedEntity.toDomain();
  }

  @Override
  public List<OutboxEvent> loadAllOrderedById() {
    return outboxEventJpaRepository.findAllByOrderByIdAsc().stream()
        .map(OutboxEventJpaEntity::toDomain)
        .toList();
  }

  public void deleteAllInBatch() {
    outboxEventJpaRepository.deleteAllInBatch();
    outboxChainTailJpaRepository.deleteAllInBatch();
  }
}
