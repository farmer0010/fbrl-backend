package com.fbrl.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fbrl.application.port.in.VerifyAuditChainUseCase.AuditChainVerificationResult;
import com.fbrl.application.port.out.LoadAllOutboxEventsPort;
import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerifyAuditChainService 단위 테스트")
class VerifyAuditChainServiceTest {

  @Mock private LoadAllOutboxEventsPort loadAllOutboxEventsPort;

  @InjectMocks private VerifyAuditChainService verifyAuditChainService;

  private OutboxEvent chainedEvent(String aggregateId, String previousHash) {
    return OutboxEvent.create("Account", aggregateId, "TRANSFER_COMPLETED", "{}")
        .chainedWith(previousHash);
  }

  @Test
  @DisplayName("이벤트가 하나도 없으면 유효한 체인(entries=0)으로 판정한다.")
  void verify_emptyChain_isValid() {
    given(loadAllOutboxEventsPort.loadAllOrderedById()).willReturn(List.of());

    AuditChainVerificationResult result = verifyAuditChainService.verify();

    assertThat(result.valid()).isTrue();
    assertThat(result.totalEntries()).isZero();
    assertThat(result.brokenAtId()).isNull();
  }

  @Test
  @DisplayName("제네시스부터 정상적으로 이어진 체인은 유효하다고 판정한다.")
  void verify_validChain() {
    OutboxEvent first = chainedEvent("111-111", OutboxEvent.GENESIS_PREVIOUS_HASH);
    OutboxEvent withId1 = withId(first, 1L);
    OutboxEvent second = chainedEvent("222-222", withId1.getEntryHash());
    OutboxEvent withId2 = withId(second, 2L);

    given(loadAllOutboxEventsPort.loadAllOrderedById()).willReturn(List.of(withId1, withId2));

    AuditChainVerificationResult result = verifyAuditChainService.verify();

    assertThat(result.valid()).isTrue();
    assertThat(result.totalEntries()).isEqualTo(2);
  }

  @Test
  @DisplayName("payload가 저장 후 변조되면 entryHash 불일치로 해당 id에서 끊어짐을 감지한다.")
  void verify_detectsTamperedPayload() {
    OutboxEvent first = withId(chainedEvent("111-111", OutboxEvent.GENESIS_PREVIOUS_HASH), 1L);
    OutboxEvent tampered =
        new OutboxEvent(
            first.getId(),
            first.getAggregateType(),
            first.getAggregateId(),
            first.getEventType(),
            "{\"tampered\":true}",
            first.getCreatedAt(),
            first.getPreviousHash(),
            first.getEntryHash());

    given(loadAllOutboxEventsPort.loadAllOrderedById()).willReturn(List.of(tampered));

    AuditChainVerificationResult result = verifyAuditChainService.verify();

    assertThat(result.valid()).isFalse();
    assertThat(result.brokenAtId()).isEqualTo(1L);
    assertThat(result.reason()).contains("변조");
  }

  @Test
  @DisplayName("previousHash 연결이 끊긴 항목이 있으면 해당 id에서 끊어짐을 감지한다.")
  void verify_detectsBrokenLink() {
    OutboxEvent first = withId(chainedEvent("111-111", OutboxEvent.GENESIS_PREVIOUS_HASH), 1L);
    OutboxEvent second = withId(chainedEvent("222-222", "f".repeat(64)), 2L);

    given(loadAllOutboxEventsPort.loadAllOrderedById()).willReturn(List.of(first, second));

    AuditChainVerificationResult result = verifyAuditChainService.verify();

    assertThat(result.valid()).isFalse();
    assertThat(result.brokenAtId()).isEqualTo(2L);
    assertThat(result.reason()).contains("연결");
  }

  private OutboxEvent withId(OutboxEvent event, Long id) {
    return new OutboxEvent(
        id,
        event.getAggregateType(),
        event.getAggregateId(),
        event.getEventType(),
        event.getPayload(),
        event.getCreatedAt(),
        event.getPreviousHash(),
        event.getEntryHash());
  }
}
