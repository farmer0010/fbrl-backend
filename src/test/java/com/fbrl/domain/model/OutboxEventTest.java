package com.fbrl.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OutboxEvent 해시체인 단위 테스트")
class OutboxEventTest {

  @Test
  @DisplayName("chainedWith()는 previousHash와 entryHash가 채워진 새 인스턴스를 반환하고 원본은 바꾸지 않는다.")
  void chainedWith_returnsNewInstanceWithHashes() {
    OutboxEvent draft = OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{}");

    OutboxEvent chained = draft.chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH);

    assertThat(draft.getPreviousHash()).isNull();
    assertThat(draft.getEntryHash()).isNull();
    assertThat(chained.getPreviousHash()).isEqualTo(OutboxEvent.GENESIS_PREVIOUS_HASH);
    assertThat(chained.getEntryHash()).isNotBlank().hasSize(64);
  }

  @Test
  @DisplayName("같은 필드값이면 entryHash가 항상 같은 값으로 재현된다.")
  void chainedWith_isDeterministic() {
    OutboxEvent draft = OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{}");

    OutboxEvent chainedA = draft.chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH);
    OutboxEvent chainedB = draft.chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH);

    assertThat(chainedA.getEntryHash()).isEqualTo(chainedB.getEntryHash());
  }

  @Test
  @DisplayName("previousHash가 다르면 entryHash도 달라진다.")
  void chainedWith_differentPreviousHash_producesDifferentEntryHash() {
    OutboxEvent draft = OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{}");

    OutboxEvent chainedFromGenesis = draft.chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH);
    OutboxEvent chainedFromOther = draft.chainedWith("f".repeat(64));

    assertThat(chainedFromGenesis.getEntryHash()).isNotEqualTo(chainedFromOther.getEntryHash());
  }

  @Test
  @DisplayName("recomputeEntryHash()는 변조되지 않은 항목에 대해 저장된 entryHash와 항상 일치한다.")
  void recomputeEntryHash_matchesStoredHash_whenUntampered() {
    OutboxEvent chained =
        OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{}")
            .chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH);

    assertThat(chained.recomputeEntryHash()).isEqualTo(chained.getEntryHash());
  }

  @Test
  @DisplayName("payload가 변조되면 recomputeEntryHash() 결과가 저장된 entryHash와 달라진다.")
  void recomputeEntryHash_detectsTamperedPayload() {
    OutboxEvent chained =
        OutboxEvent.create("Account", "111-111", "TRANSFER_COMPLETED", "{\"amount\":1000}")
            .chainedWith(OutboxEvent.GENESIS_PREVIOUS_HASH);

    OutboxEvent tampered =
        new OutboxEvent(
            chained.getId(),
            chained.getAggregateType(),
            chained.getAggregateId(),
            chained.getEventType(),
            "{\"amount\":9999999}",
            chained.getCreatedAt(),
            chained.getPreviousHash(),
            chained.getEntryHash());

    assertThat(tampered.recomputeEntryHash()).isNotEqualTo(tampered.getEntryHash());
  }
}
