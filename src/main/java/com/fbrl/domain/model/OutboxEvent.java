package com.fbrl.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

public class OutboxEvent {

  public static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);

  private final Long id;
  private final String aggregateType;
  private final String aggregateId;
  private final String eventType;
  private final String payload;
  private final Instant createdAt;
  private final String previousHash;
  private final String entryHash;
  private final String traceId;
  private final String spanId;

  public OutboxEvent(
      Long id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt,
      String previousHash,
      String entryHash) {
    this(
        id,
        aggregateType,
        aggregateId,
        eventType,
        payload,
        createdAt,
        previousHash,
        entryHash,
        null,
        null);
  }

  public OutboxEvent(
      Long id,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt,
      String previousHash,
      String entryHash,
      String traceId,
      String spanId) {
    this.id = id;
    this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType은 필수입니다.");
    this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId는 필수입니다.");
    this.eventType = Objects.requireNonNull(eventType, "eventType은 필수입니다.");
    this.payload = Objects.requireNonNull(payload, "payload는 필수입니다.");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt은 필수입니다.");
    this.previousHash = previousHash;
    this.entryHash = entryHash;
    this.traceId = traceId;
    this.spanId = spanId;
  }

  public static OutboxEvent create(
      String aggregateType, String aggregateId, String eventType, String payload) {
    return new OutboxEvent(
        null, aggregateType, aggregateId, eventType, payload, Instant.now(), null, null);
  }

  public OutboxEvent chainedWith(String previousHash) {
    Objects.requireNonNull(previousHash, "previousHash는 필수입니다.");
    String entryHash =
        computeEntryHash(aggregateType, aggregateId, eventType, payload, createdAt, previousHash);
    return new OutboxEvent(
        id,
        aggregateType,
        aggregateId,
        eventType,
        payload,
        createdAt,
        previousHash,
        entryHash,
        traceId,
        spanId);
  }

  public OutboxEvent withTraceContext(String traceId, String spanId) {
    return new OutboxEvent(
        id,
        aggregateType,
        aggregateId,
        eventType,
        payload,
        createdAt,
        previousHash,
        entryHash,
        traceId,
        spanId);
  }

  public String recomputeEntryHash() {
    return computeEntryHash(
        aggregateType, aggregateId, eventType, payload, createdAt, previousHash);
  }

  private static String computeEntryHash(
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt,
      String previousHash) {
    String raw =
        String.join(
            "|",
            aggregateType,
            aggregateId,
            eventType,
            payload,
            createdAt.toString(),
            previousHash);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
    }
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

  public String getPreviousHash() {
    return previousHash;
  }

  public String getEntryHash() {
    return entryHash;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getSpanId() {
    return spanId;
  }
}
