package com.fbrl.global.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoAopForTransaction {

  @Transactional(
      transactionManager = "demoTransactionManager",
      propagation = Propagation.REQUIRES_NEW)
  public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
    return joinPoint.proceed();
  }
}
