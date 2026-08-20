package com.fbrl.adapter.out.persistence;

import com.fbrl.application.port.out.SaveSagaPort;
import com.fbrl.domain.exception.ConcurrentSagaModificationException;
import com.fbrl.domain.exception.SagaPersistenceException;
import com.fbrl.domain.model.TransferSaga;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class SagaPersistenceAdapter implements SaveSagaPort {

  private final TransferSagaJpaRepository transferSagaJpaRepository;
  private final SagaMapper sagaMapper;

  public SagaPersistenceAdapter(
      TransferSagaJpaRepository transferSagaJpaRepository, SagaMapper sagaMapper) {
    this.transferSagaJpaRepository = transferSagaJpaRepository;
    this.sagaMapper = sagaMapper;
  }

  @Override
  public TransferSaga save(TransferSaga saga) {
    try {
      TransferSagaJpaEntity entity = sagaMapper.toEntity(saga);
      TransferSagaJpaEntity savedEntity = transferSagaJpaRepository.saveAndFlush(entity);
      return sagaMapper.toDomain(savedEntity);
    } catch (ObjectOptimisticLockingFailureException e) {
      throw new ConcurrentSagaModificationException(
          "다른 요청이 이미 이 Saga의 상태를 변경했습니다. sagaId: " + saga.getSagaId(), e);
    } catch (DataAccessException e) {
      throw new SagaPersistenceException("Saga 저장 중 인프라 예외가 발생했습니다.", e);
    }
  }
}
