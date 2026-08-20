package com.fbrl.adapter.in.web.demo;

import java.time.Instant;

public record DemoResetStatusResponse(Instant lastResetAt, Instant nextResetAt) {}
