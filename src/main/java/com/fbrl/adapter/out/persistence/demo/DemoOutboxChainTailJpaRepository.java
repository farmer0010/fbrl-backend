package com.fbrl.adapter.out.persistence.demo;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DemoOutboxChainTailJpaRepository extends JpaRepository<DemoOutboxChainTailEntity, Long> {

  @Modifying
  @Query(
      value =
          "INSERT INTO outbox_chain_tail (id, latest_entry_hash) VALUES (1, :genesisHash) "
              + "ON CONFLICT (id) DO NOTHING",
      nativeQuery = true)
  void ensureInitialized(@Param("genesisHash") String genesisHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from DemoOutboxChainTailEntity t where t.id = 1")
  Optional<DemoOutboxChainTailEntity> findForUpdate();
}
