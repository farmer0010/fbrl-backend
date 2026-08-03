package com.fbrl.adapter.in.web;

import com.fbrl.adapter.in.web.dto.ErrorResponse;
import com.fbrl.domain.exception.AccountNotFoundException;
import com.fbrl.domain.exception.DuplicateAccountNumberException;
import com.fbrl.domain.exception.InsufficientBalanceException;
import com.fbrl.domain.exception.InvalidMoneyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccountNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAccountNotFoundException(AccountNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of("ACCOUNT_NOT_FOUND", e.getMessage()));
  }

  @ExceptionHandler({InsufficientBalanceException.class, InvalidMoneyException.class})
  public ResponseEntity<ErrorResponse> handleDomainValidationException(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("INVALID_TRANSACTION", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("INVALID_INPUT", errorMessage));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("BAD_REQUEST", e.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
  }

  @ExceptionHandler(DuplicateAccountNumberException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateAccountNumberException(
      DuplicateAccountNumberException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of("DUPLICATE_ACCOUNT_NUMBER", e.getMessage()));
  }
}
