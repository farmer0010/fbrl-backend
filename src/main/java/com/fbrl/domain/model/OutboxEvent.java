package com.fbrl.domain.model;

import java.time.Instant;
import java.util.Objects;

public class OutboxEvent {

  private final Long id;
  private final String aggregateType;
  private final String aggregateId;
  private final String eventType;
  private final String payload;
  private final Instant createdAt;

  public OutboxEvent(
      Long id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt) {
    this.id = id;
    this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType은 필수입니다.");
    this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId는 필수입니다.");
    this.eventType = Objects.requireNonNull(eventType, "eventType은 필수입니다.");
    this.payload = Objects.requireNonNull(payload, "payload는 필수입니다.");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
  }

  public static OutboxEvent create(
      String aggregateType, String aggregateId, String eventType, String payload) {
    return new OutboxEvent(null, aggregateType, aggregateId, eventType, payload, Instant.now());
  }

  public Long getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
