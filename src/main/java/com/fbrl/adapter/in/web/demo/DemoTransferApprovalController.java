package com.fbrl.adapter.in.web.demo;

import com.fbrl.adapter.in.web.dto.ApprovalDecisionResponse;
import com.fbrl.adapter.in.web.dto.RejectTransferRequest;
import com.fbrl.adapter.in.web.dto.RequestTransferApprovalRequest;
import com.fbrl.adapter.in.web.dto.TransferApprovalDetailResponse;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApproveTransferCommand;
import com.fbrl.application.port.in.DemoApproveTransferUseCase;
import com.fbrl.application.port.in.DemoGetApprovalRequestUseCase;
import com.fbrl.application.port.in.DemoRejectTransferUseCase;
import com.fbrl.application.port.in.DemoRequestTransferApprovalUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/transfer-approvals")
public class DemoTransferApprovalController {

  private final DemoRequestTransferApprovalUseCase demoRequestTransferApprovalUseCase;
  private final DemoApproveTransferUseCase demoApproveTransferUseCase;
  private final DemoRejectTransferUseCase demoRejectTransferUseCase;
  private final DemoGetApprovalRequestUseCase demoGetApprovalRequestUseCase;

  public DemoTransferApprovalController(
      DemoRequestTransferApprovalUseCase demoRequestTransferApprovalUseCase,
      DemoApproveTransferUseCase demoApproveTransferUseCase,
      DemoRejectTransferUseCase demoRejectTransferUseCase,
      DemoGetApprovalRequestUseCase demoGetApprovalRequestUseCase) {
    this.demoRequestTransferApprovalUseCase = demoRequestTransferApprovalUseCase;
    this.demoApproveTransferUseCase = demoApproveTransferUseCase;
    this.demoRejectTransferUseCase = demoRejectTransferUseCase;
    this.demoGetApprovalRequestUseCase = demoGetApprovalRequestUseCase;
  }

  @PostMapping
  public ResponseEntity<ApprovalDecisionResponse> requestApproval(
      @Valid @RequestBody RequestTransferApprovalRequest request, Authentication authentication) {
    ApprovalRequestResult result =
        demoRequestTransferApprovalUseCase.requestApproval(
            request.toCommand(authentication.getName()));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }

  @GetMapping("/{requestId}")
  public ResponseEntity<TransferApprovalDetailResponse> getApprovalRequest(
      @PathVariable String requestId) {
    TransferApprovalDetailResponse response =
        TransferApprovalDetailResponse.from(
            demoGetApprovalRequestUseCase.getByRequestId(requestId));
    return ResponseEntity.ok(response);
  }

  @PostMapping("/{requestId}/approve")
  public ResponseEntity<ApprovalDecisionResponse> approve(
      @PathVariable String requestId, Authentication authentication) {
    var result =
        demoApproveTransferUseCase.approve(
            new ApproveTransferCommand(requestId, authentication.getName()));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }

  @PostMapping("/{requestId}/reject")
  public ResponseEntity<ApprovalDecisionResponse> reject(
      @PathVariable String requestId,
      @Valid @RequestBody RejectTransferRequest request,
      Authentication authentication) {
    var result =
        demoRejectTransferUseCase.reject(request.toCommand(requestId, authentication.getName()));
    return ResponseEntity.ok(new ApprovalDecisionResponse(result.requestId(), result.status()));
  }
}
