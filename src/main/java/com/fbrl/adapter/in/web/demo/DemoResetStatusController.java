package com.fbrl.adapter.in.web.demo;

import com.fbrl.application.port.out.DemoResetStatusPort;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoResetStatusController {

  private final DemoResetStatusPort demoResetStatusPort;
  private final String resetCron;

  public DemoResetStatusController(
      DemoResetStatusPort demoResetStatusPort,
      @Value("${demo.reset.cron:0 */30 * * * *}") String resetCron) {
    this.demoResetStatusPort = demoResetStatusPort;
    this.resetCron = resetCron;
  }

  @GetMapping("/reset-status")
  public ResponseEntity<DemoResetStatusResponse> getResetStatus() {
    Instant lastResetAt = demoResetStatusPort.loadLastResetAt().orElse(null);
    Instant nextResetAt = computeNextResetAt();
    return ResponseEntity.ok(new DemoResetStatusResponse(lastResetAt, nextResetAt));
  }

  private Instant computeNextResetAt() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDateTime now = LocalDateTime.now(zone);
    LocalDateTime next = CronExpression.parse(resetCron).next(now);
    if (next == null) {
      return null;
    }
    return ZonedDateTime.of(next, zone).toInstant();
  }
}
