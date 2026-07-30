package com.fbrl.global.common.aop;

import com.fbrl.global.common.annotation.DistributedLock;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DistributedLockAspect {

  private static final String LOCK_PREFIX = "LOCK:";

  private final RedissonClient redissonClient;
  private final AopForTransaction aopForTransaction;

  public DistributedLockAspect(RedissonClient redissonClient, AopForTransaction aopForTransaction) {
    this.redissonClient = redissonClient;
    this.aopForTransaction = aopForTransaction;
  }

  @Around("@annotation(com.fbrl.global.common.annotation.DistributedLock)")
  public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

    String key =
        LOCK_PREFIX
            + CustomSpringELParser.getDynamicValue(
                signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());

    RLock rLock = redissonClient.getLock(key);

    try {

      boolean available =
          rLock.tryLock(
              distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());

      if (!available) {
        throw new IllegalStateException("락 획득에 실패했습니다. Key: " + key);
      }

      return aopForTransaction.proceed(joinPoint);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("락 획득 중 인터럽트가 발생했습니다.", e);

    } finally {

      try {
        if (rLock.isLocked() && rLock.isHeldByCurrentThread()) {
          rLock.unlock();
        }
      } catch (IllegalMonitorStateException e) {
      }
    }
  }

  private static class CustomSpringELParser {

    public static Object getDynamicValue(String[] parameterNames, Object[] args, String key) {

      ExpressionParser parser = new SpelExpressionParser();
      StandardEvaluationContext context = new StandardEvaluationContext();

      for (int i = 0; i < parameterNames.length; i++) {
        context.setVariable(parameterNames[i], args[i]);
      }

      return parser.parseExpression(key).getValue(context, Object.class);
    }
  }
}
