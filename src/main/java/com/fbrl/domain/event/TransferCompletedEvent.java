package com.fbrl.domain.event;

import com.fbrl.domain.model.Money;
import java.time.Instant;

public record TransferCompletedEvent(
    String senderAccountNumber, String receiverAccountNumber, Money amount, Instant occurredAt) {}
