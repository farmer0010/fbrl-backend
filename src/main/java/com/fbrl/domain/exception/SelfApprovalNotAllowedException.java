package com.fbrl.domain.exception;

public class SelfApprovalNotAllowedException extends RuntimeException {
  public SelfApprovalNotAllowedException(String actorId) {
    super("기안자 본인은 자신이 기안한 요청을 승인/거절할 수 없습니다. actorId: " + actorId);
  }
}
