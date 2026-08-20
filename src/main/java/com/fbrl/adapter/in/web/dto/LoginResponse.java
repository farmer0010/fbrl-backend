package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.AdminRole;

public record LoginResponse(String token, AdminRole role) {}
