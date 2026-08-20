package com.fbrl.adapter.out.persistence.demo;

import java.math.BigDecimal;

record DemoLedgerBalanceDeltaProjection(
    String accountNumber, BigDecimal creditTotal, BigDecimal debitTotal) {}
