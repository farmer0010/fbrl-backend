package com.fbrl.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "k8s.leader-election")
public record LeaderElectionProperties(
    boolean enabled,
    String namespace,
    String leaseName,
    int leaseDurationSeconds,
    int renewDeadlineSeconds,
    int retryPeriodSeconds) {}
