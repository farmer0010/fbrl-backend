package com.fbrl.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "admin.initial")
public record AdminInitialCredentialsProperties(String username, String password) {}
