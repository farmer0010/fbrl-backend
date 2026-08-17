package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.ApprovalDecisionResponse;
import com.fbrl.adapter.in.web.dto.PendingApprovalResponse;
import com.fbrl.adapter.in.web.dto.RejectTransferRequest;
import com.fbrl.adapter.in.web.dto.RequestTransferApprovalRequest;
import com.fbrl.adapter.in.web.dto.TransferApprovalDetailResponse;
import com.fbrl.application.port.in.ApproveTransferUseCase;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import com.fbrl.application.port.in.GetApprovalRequestUseCase;
import com.fbrl.application.port.in.GetPendingApprovalsUseCase;
import com.fbrl.application.port.in.RejectTransferUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfer-approvals")
public class TransferApprovalController {

  private final RequestTransferApprovalUseCase requestTransferApprovalUseCase;
  private final ApproveTransferUseCase approveTransferUseCase;
  private final RejectTransferUseCase rejectTransferUseCase;
  private final GetPendingApprovalsUseCase getPendingApprovalsUseCase;
  private final GetApprovalRequestUseCase getApprovalRequestUseCase;

  public TransferApprovalController(
      RequestTransferApprovalUseCase requestTransferApprovalUseCase,
      ApproveTransferUseCase approveTransferUseCase,
      RejectTransferUseCase rejectTransferUseCase,
      GetPendingApprovalsUseCase getPendingApprovalsUseCase,
      GetApprovalRequestUseCase getApprovalRequestUseCase) {
    this.requestTransferApprovalUseCase = requestTransferApprovalUseCase;
    this.approveTransferUseCase = approveTransferUseCase;
    this.rejectTransferUseCase = rejectTransferUseCase;
    this.getPendingApprovalsUseCase = getPendingApprovalsUseCase;
    this.getApprovalRequestUseCase = getApprovalRequestUseCase;
  }

  @PostMapping
  public ResponseEntity<ApprovalDecisionResponse> requestApproval(
      @Valid @RequestBody RequestTransferApprovalRequest request, Authentication authentication) {
    ApprovalRequestResult result =
        requestTransferApprovalUseCase.requestApproval(request.toCommand(authentication.getName()));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }

  @GetMapping("/pending")
  public ResponseEntity<List<PendingApprovalResponse>> getPendingApprovals() {
    List<PendingApprovalResponse> responses =
        getPendingApprovalsUseCase.getPendingApprovals().stream()
            .map(PendingApprovalResponse::from)
            .toList();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{requestId}")
  public ResponseEntity<TransferApprovalDetailResponse> getApprovalRequest(
      @PathVariable String requestId) {
    TransferApprovalDetailResponse response =
        TransferApprovalDetailResponse.from(getApprovalRequestUseCase.getByRequestId(requestId));
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{requestId}/approve")
  public ResponseEntity<ApprovalDecisionResponse> approve(
      @PathVariable String requestId, Authentication authentication) {
    var result =
        approveTransferUseCase.approve(
            new ApproveTransferCommand(requestId, authentication.getName()));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }

  @PostMapping("/{requestId}/reject")
  public ResponseEntity<ApprovalDecisionResponse> reject(
      @PathVariable String requestId,
      @Valid @RequestBody RejectTransferRequest request,
      Authentication authentication) {
    var result =
        rejectTransferUseCase.reject(request.toCommand(requestId, authentication.getName()));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }
}
