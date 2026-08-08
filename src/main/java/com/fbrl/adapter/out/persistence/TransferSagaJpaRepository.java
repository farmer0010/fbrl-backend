package com.fbrl.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransferSagaJpaRepository extends JpaRepository<TransferSagaJpaEntity, Long> {}
