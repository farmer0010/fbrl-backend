package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.LoadApprovalRequestPort;
import com.fbrl.application.port.out.PagedResult;
import com.fbrl.application.port.out.SaveApprovalRequestPort;
import com.fbrl.domain.exception.ApprovalPersistenceException;
import com.fbrl.domain.exception.ConcurrentApprovalModificationException;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPersistenceAdapter
    implements SaveApprovalRequestPort, LoadApprovalRequestPort {

  private final TransferApprovalRequestJpaRepository transferApprovalRequestJpaRepository;
  private final ApprovalRequestMapper approvalRequestMapper;

  public ApprovalPersistenceAdapter(
      TransferApprovalRequestJpaRepository transferApprovalRequestJpaRepository,
      ApprovalRequestMapper approvalRequestMapper) {
    this.transferApprovalRequestJpaRepository = transferApprovalRequestJpaRepository;
    this.approvalRequestMapper = approvalRequestMapper;
  }

  @Override
  public TransferApprovalRequest save(TransferApprovalRequest request) {
    try {
      TransferApprovalRequestJpaEntity entity = approvalRequestMapper.toEntity(request);
      TransferApprovalRequestJpaEntity savedEntity =
          transferApprovalRequestJpaRepository.save(entity);
      return approvalRequestMapper.toDomain(savedEntity);
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new ConcurrentApprovalModificationException(
          "다른 요청이 이미 이 승인 요청의 상태를 변경했습니다. requestId: " + request.getRequestId(), e);
    } catch (DataAccessException e) {
      throw new ApprovalPersistenceException("승인 요청 저장 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public Optional<TransferApprovalRequest> loadByRequestId(String requestId) {
    return transferApprovalRequestJpaRepository
        .findByRequestId(requestId)
        .map(approvalRequestMapper::toDomain);
  }

  @Override
  public List<TransferApprovalRequest> loadByStatus(ApprovalStatus status) {
    return transferApprovalRequestJpaRepository.findByStatus(status).stream()
        .map(approvalRequestMapper::toDomain)
        .toList();
  }

  @Override
  public PagedResult<TransferApprovalRequest> search(
      ApprovalStatus status, Instant from, Instant to, int page, int size) {
    Page<TransferApprovalRequestJpaEntity> result =
        transferApprovalRequestJpaRepository.search(status, from, to, PageRequest.of(page, size));
    List<TransferApprovalRequest> items =
        result.getContent().stream().map(approvalRequestMapper::toDomain).toList();
    return new PagedResult<>(items, result.getTotalElements());
  }

  public void deleteAllInBatch() {
    transferApprovalRequestJpaRepository.deleteAllInBatch();
  }
}
