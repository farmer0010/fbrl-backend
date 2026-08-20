package com.fbrl.adapter.out.persistence.demo;

import com.fbrl.application.port.out.DemoDataWipePort;
import org.springframework.stereotype.Component;

@Component
public class DemoDataWipeAdapter implements DemoDataWipePort {

  private final DemoAccountPersistenceAdapter demoAccountPersistenceAdapter;
  private final DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter;
  private final DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter;
  private final DemoReconciliationDiscrepancyPersistenceAdapter
      demoReconciliationDiscrepancyPersistenceAdapter;
  private final DemoApprovalRequestPersistenceAdapter demoApprovalRequestPersistenceAdapter;
  private final DemoOutboxPersistenceAdapter demoOutboxPersistenceAdapter;

  public DemoDataWipeAdapter(
      DemoAccountPersistenceAdapter demoAccountPersistenceAdapter,
      DemoLedgerEntryPersistenceAdapter demoLedgerEntryPersistenceAdapter,
      DemoEodSnapshotPersistenceAdapter demoEodSnapshotPersistenceAdapter,
      DemoReconciliationDiscrepancyPersistenceAdapter
          demoReconciliationDiscrepancyPersistenceAdapter,
      DemoApprovalRequestPersistenceAdapter demoApprovalRequestPersistenceAdapter,
      DemoOutboxPersistenceAdapter demoOutboxPersistenceAdapter) {
    this.demoAccountPersistenceAdapter = demoAccountPersistenceAdapter;
    this.demoLedgerEntryPersistenceAdapter = demoLedgerEntryPersistenceAdapter;
    this.demoEodSnapshotPersistenceAdapter = demoEodSnapshotPersistenceAdapter;
    this.demoReconciliationDiscrepancyPersistenceAdapter =
        demoReconciliationDiscrepancyPersistenceAdapter;
    this.demoApprovalRequestPersistenceAdapter = demoApprovalRequestPersistenceAdapter;
    this.demoOutboxPersistenceAdapter = demoOutboxPersistenceAdapter;
  }

  @Override
  public void wipeAll() {
    demoReconciliationDiscrepancyPersistenceAdapter.deleteAllInBatch();
    demoEodSnapshotPersistenceAdapter.deleteAllInBatch();
    demoLedgerEntryPersistenceAdapter.deleteAllInBatch();
    demoApprovalRequestPersistenceAdapter.deleteAllInBatch();
    demoAccountPersistenceAdapter.deleteAllInBatch();
    demoOutboxPersistenceAdapter.deleteAllInBatch();
  }
}
