package com.fbrl.adapter.out.persistence.demo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_chain_tail")
@Getter
@NoArgsConstructor
public class DemoOutboxChainTailEntity {

  @Id private Long id;

  @Column(name = "latest_entry_hash", nullable = false, length = 64)
  private String latestEntryHash;

  void updateLatestEntryHash(String latestEntryHash) {
    this.latestEntryHash = latestEntryHash;
  }
}
