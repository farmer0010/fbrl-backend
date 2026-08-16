package com.fbrl.adapter.out.persistence;

import java.math.BigDecimal;

record LedgerBalanceDeltaProjection(
    String accountNumber, BigDecimal creditTotal, BigDecimal debitTotal) {}
