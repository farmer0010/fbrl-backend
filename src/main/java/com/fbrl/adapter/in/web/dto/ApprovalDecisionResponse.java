package com.fbrl.adapter.in.web.dto;

import com.fbrl.domain.model.ApprovalStatus;

public record ApprovalDecisionResponse(String requestId, ApprovalStatus status) {}
