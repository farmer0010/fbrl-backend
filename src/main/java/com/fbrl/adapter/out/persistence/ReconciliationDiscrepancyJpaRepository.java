package com.fbrl.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReconciliationDiscrepancyJpaRepository
    extends JpaRepository<ReconciliationDiscrepancyJpaEntity, Long> {}
