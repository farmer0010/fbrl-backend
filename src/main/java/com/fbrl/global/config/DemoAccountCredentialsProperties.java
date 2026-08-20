package com.fbrl.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.account")
public record DemoAccountCredentialsProperties(String username, String password) {}
