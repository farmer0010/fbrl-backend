package com.fbrl.application.port.out;

import java.time.LocalDateTime;

public record BatchJobExecutionSummary(
    String jobName,
    String status,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String exitDescription) {}
