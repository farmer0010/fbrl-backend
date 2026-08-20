package com.fbrl.adapter.out.persistence.demo;

import org.springframework.data.jpa.repository.JpaRepository;

interface DemoReconciliationDiscrepancyJpaRepository
    extends JpaRepository<DemoReconciliationDiscrepancyEntity, Long> {}
