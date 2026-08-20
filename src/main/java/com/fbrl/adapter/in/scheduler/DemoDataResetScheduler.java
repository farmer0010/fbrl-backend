package com.fbrl.adapter.in.scheduler;

import com.fbrl.application.service.DemoDataResetService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoDataResetScheduler {
  private static final Logger log = LoggerFactory.getLogger(DemoDataResetScheduler.class);

  private final DemoDataResetService demoDataResetService;

  public DemoDataResetScheduler(DemoDataResetService demoDataResetService) {
    this.demoDataResetService = demoDataResetService;
  }

  @Scheduled(cron = "${demo.reset.cron:0 */30 * * * *}")
  @SchedulerLock(name = "demoDataReset", lockAtMostFor = "10m", lockAtLeastFor = "5s")
  public void triggerReset() {
    LockAssert.assertLocked();

    try {
      demoDataResetService.reset();
    } catch (Exception e) {
      log.error("데모 데이터 리셋 중 오류가 발생했습니다.", e);
    }
  }
}
