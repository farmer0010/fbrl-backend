package com.fbrl.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fbrl.application.port.out.PagedResult;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.ExecutionStatus;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("ApprovalPersistenceAdapter search() 필터/페이지네이션 테스트")
class ApprovalPersistenceAdapterTest {

  @Autowired private ApprovalPersistenceAdapter approvalPersistenceAdapter;

  private Instant base;

  @BeforeEach
  void setUp() {
    approvalPersistenceAdapter.deleteAllInBatch();
    base = Instant.parse("2026-08-01T00:00:00Z");

    save(ApprovalStatus.PENDING, base);
    save(ApprovalStatus.APPROVED, base.plus(1, ChronoUnit.DAYS));
    save(ApprovalStatus.REJECTED, base.plus(2, ChronoUnit.DAYS));
    save(ApprovalStatus.PENDING, base.plus(10, ChronoUnit.DAYS));
  }

  private void save(ApprovalStatus status, Instant requestedAt) {
    TransferApprovalRequest request =
        TransferApprovalRequest.reconstruct(
            null,
            UUID.randomUUID().toString(),
            "maker-1",
            status == ApprovalStatus.PENDING ? null : "checker-1",
            "111-111",
            "222-222",
            Money.wons(20_000_000),
            status,
            status == ApprovalStatus.REJECTED ? "테스트 거절 사유" : null,
            ExecutionStatus.NOT_APPLICABLE,
            null,
            requestedAt,
            status == ApprovalStatus.PENDING ? null : requestedAt,
            null);
    approvalPersistenceAdapter.save(request);
  }

  @Test
  @DisplayName("status가 null이면 기간 내 전체 상태를 반환한다.")
  void search_statusNull_returnsAllStatusesWithinPeriod() {
    PagedResult<TransferApprovalRequest> result =
        approvalPersistenceAdapter.search(null, base, base.plus(3, ChronoUnit.DAYS), 0, 20);

    assertThat(result.totalElements()).isEqualTo(3);
    assertThat(result.items()).hasSize(3);
  }

  @Test
  @DisplayName("status를 지정하면 해당 상태만 반환한다.")
  void search_withStatus_filtersByStatus() {
    PagedResult<TransferApprovalRequest> result =
        approvalPersistenceAdapter.search(
            ApprovalStatus.APPROVED, base, base.plus(30, ChronoUnit.DAYS), 0, 20);

    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.items().get(0).getStatus()).isEqualTo(ApprovalStatus.APPROVED);
  }

  @Test
  @DisplayName("from/to 범위 밖의 requestedAt은 제외된다.")
  void search_excludesEntriesOutsideDateRange() {
    PagedResult<TransferApprovalRequest> result =
        approvalPersistenceAdapter.search(
            null, base.plus(5, ChronoUnit.DAYS), base.plus(30, ChronoUnit.DAYS), 0, 20);

    assertThat(result.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("전체 건수보다 큰 페이지를 요청하면 빈 content를 반환하되 totalElements는 정확하다.")
  void search_pageBeyondTotal_returnsEmptyContent() {
    PagedResult<TransferApprovalRequest> result =
        approvalPersistenceAdapter.search(null, base, base.plus(30, ChronoUnit.DAYS), 5, 20);

    assertThat(result.items()).isEmpty();
    assertThat(result.totalElements()).isEqualTo(4);
  }
}
