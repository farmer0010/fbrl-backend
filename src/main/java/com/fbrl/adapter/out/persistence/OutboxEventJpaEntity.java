package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.OutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor
public class OutboxEventJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, length = 100)
  private String aggregateId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "text")
  private String payload;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  private OutboxEventJpaEntity(
      Long id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt) {

    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.createdAt = createdAt;
  }

  static OutboxEventJpaEntity fromDomain(OutboxEvent outboxEvent) {
    return new OutboxEventJpaEntity(
        outboxEvent.getId(),
        outboxEvent.getAggregateType(),
        outboxEvent.getAggregateId(),
        outboxEvent.getEventType(),
        outboxEvent.getPayload(),
        outboxEvent.getCreatedAt());
  }

  OutboxEvent toDomain() {
    return new OutboxEvent(id, aggregateType, aggregateId, eventType, payload, createdAt);
  }
}
