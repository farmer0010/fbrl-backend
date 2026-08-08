package com.fbrl.adapter.out.persistence;

import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferSaga;
import org.springframework.stereotype.Component;

@Component
public class SagaMapper {

  public TransferSaga toDomain(TransferSagaJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return TransferSaga.reconstruct(
        entity.getId(),
        entity.getSagaId(),
        entity.getFromAccountNumber(),
        entity.getToAccountNumber(),
        Money.of(entity.getAmount()),
        entity.getStatus(),
        entity.getStartedAt(),
        entity.getUpdatedAt(),
        entity.getVersion());
  }

  public TransferSagaJpaEntity toEntity(TransferSaga domain) {
    if (domain == null) {
      return null;
    }
    return new TransferSagaJpaEntity(
        domain.getId(),
        domain.getSagaId(),
        domain.getFromAccountNumber(),
        domain.getToAccountNumber(),
        domain.getAmount().getAmount(),
        domain.getStatus(),
        domain.getStartedAt(),
        domain.getUpdatedAt(),
        domain.getVersion());
  }
}
