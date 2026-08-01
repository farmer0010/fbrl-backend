package com.fbrl.global.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD) // 매서드 상단에만 붙일수 있도록 지정
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 어노테이션 정보가 유지되도록 지정
public @interface DistributedLock {
  String key();

  TimeUnit timeUnit() default TimeUnit.SECONDS;

  long waitTime() default 5L;

  long leaseTime() default 3L;
}
