package com.fbrl.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fbrl.adapter.in.web.dto.ApproveTransferRequest;
import com.fbrl.adapter.in.web.dto.RejectTransferRequest;
import com.fbrl.adapter.in.web.dto.RequestTransferApprovalRequest;
import com.fbrl.application.port.in.ApproveTransferUseCase;
import com.fbrl.application.port.in.ApproveTransferUseCase.ApprovalDecisionResult;
import com.fbrl.application.port.in.GetPendingApprovalsUseCase;
import com.fbrl.application.port.in.RejectTransferUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase;
import com.fbrl.application.port.in.RequestTransferApprovalUseCase.ApprovalRequestResult;
import com.fbrl.domain.exception.ApprovalNotRequiredException;
import com.fbrl.domain.exception.SelfApprovalNotAllowedException;
import com.fbrl.domain.model.ApprovalStatus;
import com.fbrl.domain.model.Money;
import com.fbrl.domain.model.TransferApprovalRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@DisplayName("TransferApprovalController 단위 테스트")
class TransferApprovalControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper = new ObjectMapper();

  private RequestTransferApprovalUseCase requestTransferApprovalUseCase;
  private ApproveTransferUseCase approveTransferUseCase;
  private RejectTransferUseCase rejectTransferUseCase;
  private GetPendingApprovalsUseCase getPendingApprovalsUseCase;

  @BeforeEach
  void setUp() {
    requestTransferApprovalUseCase = Mockito.mock(RequestTransferApprovalUseCase.class);
    approveTransferUseCase = Mockito.mock(ApproveTransferUseCase.class);
    rejectTransferUseCase = Mockito.mock(RejectTransferUseCase.class);
    getPendingApprovalsUseCase = Mockito.mock(GetPendingApprovalsUseCase.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new TransferApprovalController(
                    requestTransferApprovalUseCase,
                    approveTransferUseCase,
                    rejectTransferUseCase,
                    getPendingApprovalsUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("승인이 필요한 금액으로 요청하면 PENDING 상태와 함께 200 OK를 반환한다.")
  void requestApprovalSuccess() throws Exception {
    RequestTransferApprovalRequest request =
        new RequestTransferApprovalRequest(
            "maker-1", "111-111", "222-222", BigDecimal.valueOf(20_000_000));

    given(requestTransferApprovalUseCase.requestApproval(any()))
        .willReturn(new ApprovalRequestResult("req-1", ApprovalStatus.PENDING));

    mockMvc
        .perform(
            post("/api/v1/transfer-approvals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value("req-1"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  @DisplayName("threshold 미만 금액으로 요청하면 ApprovalNotRequiredException이 터져 400 Bad Request를 반환한다.")
  void requestApprovalBelowThreshold() throws Exception {
    RequestTransferApprovalRequest request =
        new RequestTransferApprovalRequest(
            "maker-1", "111-111", "222-222", BigDecimal.valueOf(1_000_000));

    willThrow(new ApprovalNotRequiredException(BigDecimal.valueOf(1_000_000)))
        .given(requestTransferApprovalUseCase)
        .requestApproval(any());

    mockMvc
        .perform(
            post("/api/v1/transfer-approvals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("APPROVAL_NOT_REQUIRED"));
  }

  @Test
  @DisplayName("대기 중인 승인 요청 목록을 조회하면 200 OK와 함께 목록을 반환한다.")
  void getPendingApprovalsSuccess() throws Exception {
    TransferApprovalRequest pending =
        TransferApprovalRequest.request("maker-1", "111-111", "222-222", Money.wons(20_000_000));
    given(getPendingApprovalsUseCase.getPendingApprovals()).willReturn(List.of(pending));

    mockMvc
        .perform(get("/api/v1/transfer-approvals/pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].requestId").value(pending.getRequestId()))
        .andExpect(jsonPath("$[0].makerId").value("maker-1"));
  }

  @Test
  @DisplayName("정상 승인 요청 시 APPROVED 상태와 함께 200 OK를 반환한다.")
  void approveSuccess() throws Exception {
    ApproveTransferRequest request = new ApproveTransferRequest("checker-1");

    given(approveTransferUseCase.approve(any()))
        .willReturn(new ApprovalDecisionResult("req-1", ApprovalStatus.APPROVED));

    mockMvc
        .perform(
            post("/api/v1/transfer-approvals/req-1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
  }

  @Test
  @DisplayName("기안자 본인이 승인을 시도하면 SelfApprovalNotAllowedException이 터져 400 Bad Request를 반환한다.")
  void approveBySameMaker() throws Exception {
    ApproveTransferRequest request = new ApproveTransferRequest("maker-1");

    willThrow(new SelfApprovalNotAllowedException("maker-1"))
        .given(approveTransferUseCase)
        .approve(any());

    mockMvc
        .perform(
            post("/api/v1/transfer-approvals/req-1/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SELF_APPROVAL_NOT_ALLOWED"));
  }

  @Test
  @DisplayName("거절 사유 없이 거절을 요청하면 @Valid 검증에 실패하여 400 Bad Request를 반환한다.")
  void rejectValidationFailureWhenReasonBlank() throws Exception {
    RejectTransferRequest request = new RejectTransferRequest("checker-1", "");

    mockMvc
        .perform(
            post("/api/v1/transfer-approvals/req-1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  @DisplayName("정상 거절 요청 시 REJECTED 상태와 함께 200 OK를 반환한다.")
  void rejectSuccess() throws Exception {
    RejectTransferRequest request = new RejectTransferRequest("checker-1", "한도 초과 우려");

    given(rejectTransferUseCase.reject(any()))
        .willReturn(
            new RejectTransferUseCase.ApprovalDecisionResult("req-1", ApprovalStatus.REJECTED));

    mockMvc
        .perform(
            post("/api/v1/transfer-approvals/req-1/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
  }
}
