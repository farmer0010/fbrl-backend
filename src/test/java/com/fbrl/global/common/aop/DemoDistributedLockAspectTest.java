package com.fbrl.global.common.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fbrl.global.common.annotation.DemoDistributedLock;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemoDistributedLockAspect 단위 테스트 — 락 키 네임스페이스 분리 확인")
class DemoDistributedLockAspectTest {

  @Mock private RedissonClient redissonClient;
  @Mock private DemoAopForTransaction demoAopForTransaction;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private MethodSignature methodSignature;
  @Mock private RLock rLock;

  private static class Sample {
    @DemoDistributedLock(key = "#accountNumber")
    void transfer(String accountNumber) {}
  }

  @Test
  @DisplayName("락 키가 DEMO-LOCK: 접두사로 생성되고, 운영 락 키(LOCK: 접두사)와 겹치지 않는다")
  void generatesDemoPrefixedLockKey() throws Throwable {
    DemoDistributedLockAspect demoDistributedLockAspect =
        new DemoDistributedLockAspect(redissonClient, demoAopForTransaction);

    Method method = Sample.class.getDeclaredMethod("transfer", String.class);
    given(joinPoint.getSignature()).willReturn(methodSignature);
    given(methodSignature.getMethod()).willReturn(method);
    given(methodSignature.getParameterNames()).willReturn(new String[] {"accountNumber"});
    given(joinPoint.getArgs()).willReturn(new Object[] {"111-111"});
    given(redissonClient.getLock(anyString())).willReturn(rLock);
    given(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).willReturn(true);
    given(demoAopForTransaction.proceed(joinPoint)).willReturn(null);

    demoDistributedLockAspect.lock(joinPoint);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(redissonClient).getLock(keyCaptor.capture());

    String generatedKey = keyCaptor.getValue();
    assertThat(generatedKey).isEqualTo("DEMO-LOCK:111-111");
    assertThat(generatedKey).startsWith("DEMO-LOCK:");
    assertThat(generatedKey).isNotEqualTo("LOCK:111-111");
  }
}
