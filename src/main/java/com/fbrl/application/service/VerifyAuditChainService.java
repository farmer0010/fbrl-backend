package com.fbrl.application.service;

import com.fbrl.application.port.in.VerifyAuditChainUseCase;
import com.fbrl.application.port.out.LoadAllOutboxEventsPort;
import com.fbrl.domain.model.OutboxEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VerifyAuditChainService implements VerifyAuditChainUseCase {

  private final LoadAllOutboxEventsPort loadAllOutboxEventsPort;

  @Override
  @Transactional(readOnly = true)
  public AuditChainVerificationResult verify() {
    List<OutboxEvent> events = loadAllOutboxEventsPort.loadAllOrderedById();

    String expectedPreviousHash = OutboxEvent.GENESIS_PREVIOUS_HASH;
    for (OutboxEvent event : events) {
      if (!expectedPreviousHash.equals(event.getPreviousHash())) {
        return AuditChainVerificationResult.broken(
            events.size(), event.getId(), "이전 항목과의 연결이 끊어졌습니다. previousHash가 일치하지 않습니다.");
      }

      if (!event.getEntryHash().equals(event.recomputeEntryHash())) {
        return AuditChainVerificationResult.broken(
            events.size(), event.getId(), "항목 내용이 변조되었습니다. entryHash가 일치하지 않습니다.");
      }

      expectedPreviousHash = event.getEntryHash();
    }

    return AuditChainVerificationResult.valid(events.size());
  }
}
