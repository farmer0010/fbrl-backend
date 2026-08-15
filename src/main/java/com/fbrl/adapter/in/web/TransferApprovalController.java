package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.ApprovalDecisionResponse;
import com.fbrl.adapter.in.web.dto.ApproveTransferRequest;
import com.fbrl.adapter.in.web.dto.PendingApprovalResponse;
import com.fbrl.adapter.in.web.dto.RejectTransferRequest;
import com.fbrl.adapter.in.web.dto.RequestTransferApprovalRequest;
import com.fbrl.application.port.in.ApproveTransferUseCase;
import com.fbrl.application.port.in.GetPendingApprovalsUseCase;
import com.fbrl.application.port.in.RejectTransferUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
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

  public TransferApprovalController(
      RequestTransferApprovalUseCase requestTransferApprovalUseCase,
      ApproveTransferUseCase approveTransferUseCase,
      RejectTransferUseCase rejectTransferUseCase,
      GetPendingApprovalsUseCase getPendingApprovalsUseCase) {
    this.requestTransferApprovalUseCase = requestTransferApprovalUseCase;
    this.approveTransferUseCase = approveTransferUseCase;
    this.rejectTransferUseCase = rejectTransferUseCase;
    this.getPendingApprovalsUseCase = getPendingApprovalsUseCase;
  }

  @PostMapping
  public ResponseEntity<ApprovalDecisionResponse> requestApproval(
      @Valid @RequestBody RequestTransferApprovalRequest request) {
    ApprovalRequestResult result =
        requestTransferApprovalUseCase.requestApproval(request.toCommand());
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

  @PostMapping("/{requestId}/approve")
  public ResponseEntity<ApprovalDecisionResponse> approve(
      @PathVariable String requestId, @Valid @RequestBody ApproveTransferRequest request) {
    var result = approveTransferUseCase.approve(request.toCommand(requestId));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }

  @PostMapping("/{requestId}/reject")
  public ResponseEntity<ApprovalDecisionResponse> reject(
      @PathVariable String requestId, @Valid @RequestBody RejectTransferRequest request) {
    var result = rejectTransferUseCase.reject(request.toCommand(requestId));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }
}
