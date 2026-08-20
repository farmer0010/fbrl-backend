package com.fbrl.adapter.out.persistence.demo;

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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@Qualifier("demo")
public class DemoApprovalRequestPersistenceAdapter
    implements SaveApprovalRequestPort, LoadApprovalRequestPort {

  private final DemoApprovalRequestJpaRepository demoApprovalRequestJpaRepository;
  private final DemoApprovalRequestMapper demoApprovalRequestMapper;

  public DemoApprovalRequestPersistenceAdapter(
      DemoApprovalRequestJpaRepository demoApprovalRequestJpaRepository,
      DemoApprovalRequestMapper demoApprovalRequestMapper) {
    this.demoApprovalRequestJpaRepository = demoApprovalRequestJpaRepository;
    this.demoApprovalRequestMapper = demoApprovalRequestMapper;
  }

  @Override
  public TransferApprovalRequest save(TransferApprovalRequest request) {
    try {
      DemoApprovalRequestEntity entity = demoApprovalRequestMapper.toEntity(request);
      DemoApprovalRequestEntity savedEntity = demoApprovalRequestJpaRepository.save(entity);
      return demoApprovalRequestMapper.toDomain(savedEntity);
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new ConcurrentApprovalModificationException(
          "다른 요청이 이미 이 승인 요청의 상태를 변경했습니다. requestId: " + request.getRequestId(), e);
    } catch (DataAccessException e) {
      throw new ApprovalPersistenceException("데모 승인 요청 저장 중 인프라 예외가 발생했습니다.", e);
    }
  }

  @Override
  public Optional<TransferApprovalRequest> loadByRequestId(String requestId) {
    return demoApprovalRequestJpaRepository
        .findByRequestId(requestId)
        .map(demoApprovalRequestMapper::toDomain);
  }

  @Override
  public List<TransferApprovalRequest> loadByStatus(ApprovalStatus status) {
    return demoApprovalRequestJpaRepository.findByStatus(status).stream()
        .map(demoApprovalRequestMapper::toDomain)
        .toList();
  }

  @Override
  public PagedResult<TransferApprovalRequest> search(
      ApprovalStatus status, Instant from, Instant to, int page, int size) {
    Page<DemoApprovalRequestEntity> result =
        demoApprovalRequestJpaRepository.search(status, from, to, PageRequest.of(page, size));
    List<TransferApprovalRequest> items =
        result.getContent().stream().map(demoApprovalRequestMapper::toDomain).toList();
    return new PagedResult<>(items, result.getTotalElements());
  }

  public void deleteAllInBatch() {
    demoApprovalRequestJpaRepository.deleteAllInBatch();
  }
}
